package kz.abac;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import kz.abac.audit.AuditRecord;
import kz.abac.audit.AuditedEngine;
import kz.abac.repo.InMemoryAuditRepository;
import org.junit.jupiter.api.Test;

class BatchEvaluatorTest {
    private static final PolicyEngine ENGINE =
            new PolicyEngine(PolicyLoader.load(Path.of("policies-hospital.json")));

    private static AttributeContext req(String role, String action) {
        return AttributeContext.builder().role(role).action(action).device("corporate")
                .time("10:00").attribute("department", "icu").attribute("record_department", "icu").build();
    }

    @Test
    void resultsKeepRequestOrder() {
        List<AttributeContext> requests = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            requests.add(i % 2 == 0 ? req("doctor", "read") : req("registrar", "read"));
        }
        try (var batch = new BatchEvaluator(ENGINE::evaluate, 8)) {
            List<Decision> result = batch.evaluateAll(requests);
            assertEquals(200, result.size());
            for (int i = 0; i < result.size(); i++) {
                assertEquals(i % 2 == 0 ? Effect.PERMIT : Effect.DENY, result.get(i).effect());
            }
        }
    }

    @Test
    void parallelAuditWritesEveryRecordExactlyOnce() {
        var repo = new InMemoryAuditRepository();
        var audited = new AuditedEngine(ENGINE, repo);
        List<AttributeContext> requests = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            requests.add(req("doctor", "read"));
        }
        try (var batch = new BatchEvaluator(audited::evaluate, 8)) {
            batch.evaluateAll(requests);
        }
        assertEquals(500L, repo.count());
        long distinctIds = repo.findAll().stream().map(AuditRecord::id).distinct().count();
        assertEquals(500L, distinctIds);
    }

    @Test
    void workerExceptionIsPropagated() {
        try (var batch = new BatchEvaluator(ctx -> { throw new IllegalStateException("boom"); }, 2)) {
            assertThrows(IllegalStateException.class, () -> batch.evaluateAll(List.of(req("doctor", "read"))));
        }
    }

    @Test
    void invalidThreadCountRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BatchEvaluator(ENGINE::evaluate, 0));
    }

    @Test
    void auditedEngineStoresDecisionWithClockTime() {
        var repo = new InMemoryAuditRepository();
        Instant fixed = Instant.parse("2026-05-05T05:05:05Z");
        var audited = new AuditedEngine(ENGINE, repo, Clock.fixed(fixed, ZoneOffset.UTC));

        Decision d = audited.evaluate(req("doctor", "read"));

        AuditRecord saved = repo.findAll().get(0);
        assertEquals(fixed, saved.timestamp());
        assertEquals(d.effect(), saved.effect());
        assertEquals(d.policyId(), saved.policyId());
        assertEquals("doctor", saved.role());
    }
}

