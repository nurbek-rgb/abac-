package kz.abac;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import kz.abac.audit.*;
import kz.abac.repo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AuditIntegrationTest {
    private static AuditRecord record(String role,Effect effect,String policy) {
        return new AuditRecord(null,Instant.parse("2026-11-27T00:00:00Z"),Map.of("role",role,"action","read"),effect,policy,"Себеп, \"қазақша\"\nnext");
    }
    @Test void crudUpdateWorksInBothRepositories(@TempDir Path dir) {
        var memory=new InMemoryAuditRepository();
        try(var sqlite=new SqliteAuditRepository("jdbc:sqlite:"+dir.resolve("crud.db"))) {
            for(Repository<AuditRecord> repo:List.of(memory,sqlite)) {
                var first=repo.save(record("doctor",Effect.PERMIT,"H1"));
                var changed=new AuditRecord(first.id(),first.timestamp(),first.attributes(),Effect.DENY,null,"changed");
                repo.save(changed);assertEquals(1,repo.count());assertEquals(changed,repo.findById(first.id()).orElseThrow());
                assertThrows(AuditException.class,()->repo.save(changed.withId(999)));
            }
        }
    }
    @Test void parallelSQLiteWritesAreDurableAndUnique(@TempDir Path dir) {
        String url="jdbc:sqlite:"+dir.resolve("parallel.db");
        var engine=new PolicyEngine(List.of(new Policy("P","",Effect.PERMIT,ctx->MatchResult.TRUE)));
        try(var repo=new SqliteAuditRepository(url);var batch=new BatchEvaluator(new AuditedEngine(engine,repo)::evaluate,8)) {
            var inputs=IntStream.range(0,200).mapToObj(i->AttributeContext.builder().role("doctor").attribute("requestId",""+i).build()).toList();
            assertTrue(batch.evaluateAll(inputs).stream().allMatch(Decision::isPermit));
            assertEquals(200,repo.count());assertEquals(200,repo.findAll().stream().map(AuditRecord::id).distinct().count());
            assertEquals(200,repo.findAll().stream().map(r->r.attributes().get("requestId")).distinct().count());
        }
        try(var reopened=new SqliteAuditRepository(url)) { assertEquals(200,reopened.count()); }
    }
    @Test void auditFailureNeverReturnsPermission(@TempDir Path dir) {
        var repo=new SqliteAuditRepository("jdbc:sqlite:"+dir.resolve("closed.db"));repo.close();
        var engine=new PolicyEngine(List.of(new Policy("P","",Effect.PERMIT,ctx->MatchResult.TRUE)));
        assertThrows(AuditException.class,()->new AuditedEngine(engine,repo).evaluate(AttributeContext.builder().build()));
    }
    @Test void jsonCsvAndStreamReportsPreserveData(@TempDir Path dir) throws Exception {
        var records=List.of(record("doctor",Effect.PERMIT,"H3").withId(1),record("nurse",Effect.DENY,null).withId(2));
        AuditExporter.toCsv(records,dir.resolve("audit.csv"));AuditExporter.toJson(records,dir.resolve("audit.json"));
        var parsed=new ObjectMapper().readTree(Files.readString(dir.resolve("audit.json")));
        assertEquals(records.get(0).reason(),parsed.get(0).get("reason").asText());assertTrue(parsed.get(1).get("policyId").isNull());
        String csv=Files.readString(dir.resolve("audit.csv"));assertTrue(csv.contains("\"Себеп, \"\"қазақша\"\"\nnext\""));
        AuditStats stats=AuditStats.from(records);
        assertEquals(2,stats.total());assertEquals(1,stats.permits());assertEquals(1,stats.denies());assertEquals(1,stats.emergencyPermits());
        assertEquals(1L,stats.byRole().get("doctor"));assertEquals(2,stats.roles().size());
    }
    @Test void csvNeutralizesFormulasAndSqlTreatsInputAsData(@TempDir Path dir) throws Exception {
        String role="doctor'); DROP TABLE audit_records; --";
        try(var repo=new SqliteAuditRepository("jdbc:sqlite:"+dir.resolve("safe.db"))) {
            var saved=repo.save(record(role,Effect.DENY,null));assertEquals(role,repo.findById(saved.id()).orElseThrow().role());assertEquals(1,repo.count());
        }
        AuditExporter.toCsv(List.of(record("=1+1",Effect.DENY,null)),dir.resolve("safe.csv"));
        assertTrue(Files.readString(dir.resolve("safe.csv")).contains("\"'=1+1\""));
    }
    @Test void closedBatchRejectsNewWork() {
        var batch=new BatchEvaluator(ctx->new Decision(Effect.DENY,null,"test"),1);batch.close();
        assertThrows(java.util.concurrent.RejectedExecutionException.class,()->batch.evaluateAll(List.of(AttributeContext.builder().build())));
    }
}
