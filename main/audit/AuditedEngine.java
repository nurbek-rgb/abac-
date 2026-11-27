package kz.abac.audit;

import java.time.Clock;
import java.util.Objects;
import kz.abac.AttributeContext;
import kz.abac.Decision;
import kz.abac.PolicyEngine;
import kz.abac.repo.Repository;

/** A decision is returned only after its audit entry has been persisted. */
public final class AuditedEngine {
    private final PolicyEngine engine;
    private final Repository<AuditRecord> repository;
    private final Clock clock;
    public AuditedEngine(PolicyEngine engine, Repository<AuditRecord> repository) { this(engine,repository,Clock.systemUTC()); }
    public AuditedEngine(PolicyEngine engine, Repository<AuditRecord> repository, Clock clock) {
        this.engine=Objects.requireNonNull(engine); this.repository=Objects.requireNonNull(repository); this.clock=Objects.requireNonNull(clock);
    }
    public Decision evaluate(AttributeContext ctx) {
        Objects.requireNonNull(ctx,"context");
        Decision decision=engine.evaluate(ctx);
        repository.save(new AuditRecord(null,clock.instant(),ctx.attributes(),decision.effect(),decision.policyId(),decision.reason()));
        return decision;
    }
}
