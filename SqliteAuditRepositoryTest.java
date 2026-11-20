package kz.abac.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import kz.abac.Effect;
import kz.abac.audit.AuditRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** SQLite драйвері (sqlite-jdbc) Maven арқылы жүктеледі, сондықтан тест mvn test-те жұмыс істейді. */
class SqliteAuditRepositoryTest {

    private static AuditRecord rec(String role, Effect effect, String policy) {
        return new AuditRecord(null, Instant.parse("2026-01-01T10:00:00Z"),
                Map.of("role", role, "action", "read"), effect, policy, "себеп, үтірмен");
    }

    @Test
    void saveReadDelete(@TempDir Path dir) {
        try (var repo = new SqliteAuditRepository("jdbc:sqlite:" + dir.resolve("t.db"))) {
            AuditRecord saved = repo.save(rec("doctor", Effect.PERMIT, "H1"));
            assertNotNull(saved.id());
            assertEquals(saved, repo.findById(saved.id()).orElseThrow());
            assertEquals(1L, repo.count());
            assertTrue(repo.deleteById(saved.id()));
            assertEquals(0L, repo.count());
        }
    }

    @Test
    void nullPolicyIdIsStoredAsNull(@TempDir Path dir) {
        try (var repo = new SqliteAuditRepository("jdbc:sqlite:" + dir.resolve("t.db"))) {
            long id = repo.save(rec("nurse", Effect.DENY, null)).id();
            assertNull(repo.findById(id).orElseThrow().policyId());
        }
    }

    @Test
    void dataSurvivesReopen(@TempDir Path dir) {
        String url = "jdbc:sqlite:" + dir.resolve("t.db");
        try (var repo = new SqliteAuditRepository(url)) {
            repo.save(rec("doctor", Effect.PERMIT, "H1"));
        }
        try (var repo = new SqliteAuditRepository(url)) {
            assertEquals(1, repo.findAll().size());
        }
    }

    @Test
    void invalidUrlGivesClearError() {
        assertThrows(kz.abac.audit.AuditException.class, () -> new SqliteAuditRepository("jdbc:nodb:x"));
    }
}
