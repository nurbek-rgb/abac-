package kz.abac.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import kz.abac.Effect;
import kz.abac.audit.AuditRecord;
import org.junit.jupiter.api.Test;

class InMemoryAuditRepositoryTest {

    private static AuditRecord rec(String role, Effect effect) {
        return new AuditRecord(null, Instant.parse("2026-01-01T10:00:00Z"), Map.of("role", role), effect,
                effect == Effect.PERMIT ? "P1" : null, "r");
    }

    @Test
    void saveAssignsIncreasingIds() {
        var repo = new InMemoryAuditRepository();
        assertEquals(1L, repo.save(rec("a", Effect.PERMIT)).id());
        assertEquals(2L, repo.save(rec("b", Effect.DENY)).id());
        assertEquals(2L, repo.count());
    }

    @Test
    void findByIdAndDelete() {
        var repo = new InMemoryAuditRepository();
        long id = repo.save(rec("a", Effect.PERMIT)).id();
        assertTrue(repo.findById(id).isPresent());
        assertTrue(repo.deleteById(id));
        assertFalse(repo.deleteById(id));
        assertTrue(repo.findById(id).isEmpty());
    }

    @Test
    void findWhereFilters() {
        var repo = new InMemoryAuditRepository();
        repo.save(rec("a", Effect.PERMIT));
        repo.save(rec("b", Effect.DENY));
        repo.save(rec("c", Effect.DENY));
        assertEquals(2, repo.findWhere(r -> r.effect() == Effect.DENY).size());
    }
}

