package kz.abac.repo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kz.abac.audit.AuditException;
import kz.abac.audit.AuditRecord;

/** All operations are synchronized; snapshots cannot mutate stored records. */
public final class InMemoryAuditRepository implements Repository<AuditRecord> {
    private final Map<Long,AuditRecord> records = new LinkedHashMap<>();
    private long nextId = 1;
    @Override public synchronized AuditRecord save(AuditRecord record) {
        if (record.id() != null && !records.containsKey(record.id())) throw new AuditException("Unknown audit ID");
        AuditRecord saved = record.id() == null ? record.withId(nextId++) : record;
        records.put(saved.id(),saved);
        return saved;
    }
    @Override public synchronized Optional<AuditRecord> findById(long id) { return Optional.ofNullable(records.get(id)); }
    @Override public synchronized List<AuditRecord> findAll() { return List.copyOf(records.values()); }
    @Override public synchronized boolean deleteById(long id) { return records.remove(id) != null; }
    @Override public synchronized long count() { return records.size(); }
}
