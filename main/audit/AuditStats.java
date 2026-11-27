package kz.abac.audit;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kz.abac.Effect;

/** Stream API aggregation over an immutable repository snapshot. */
public record AuditStats(long total, long permits, long denies, long emergencyPermits,
                         Map<String,Long> byRole, Set<String> roles) {
    public AuditStats { byRole=Map.copyOf(byRole); roles=Set.copyOf(roles); }
    public static AuditStats from(List<AuditRecord> records) {
        long permits=records.stream().filter(r->r.effect()==Effect.PERMIT).count();
        long emergency=records.stream().filter(r->r.effect()==Effect.PERMIT && "H3".equals(r.policyId())).count();
        return new AuditStats(records.size(),permits,records.size()-permits,emergency,
                records.stream().collect(Collectors.groupingBy(AuditRecord::role,Collectors.counting())),
                records.stream().map(AuditRecord::role).collect(Collectors.toSet()));
    }
}
