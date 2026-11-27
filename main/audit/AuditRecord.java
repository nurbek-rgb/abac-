package kz.abac.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import kz.abac.Effect;

public record AuditRecord(Long id, Instant timestamp, Map<String,String> attributes,
                          Effect effect, String policyId, String reason) {
    public AuditRecord {
        if (id != null && id <= 0) throw new IllegalArgumentException("ID must be positive");
        Objects.requireNonNull(timestamp, "timestamp");
        attributes = Map.copyOf(attributes);
        if (effect != Effect.PERMIT && effect != Effect.DENY) throw new IllegalArgumentException("Final effect required");
        Objects.requireNonNull(reason, "reason");
    }
    public String role() { return attributes.getOrDefault("role", ""); }
    public String action() { return attributes.getOrDefault("action", ""); }
    public AuditRecord withId(long newId) { return new AuditRecord(newId,timestamp,attributes,effect,policyId,reason); }
}
