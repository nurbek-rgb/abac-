package kz.abac;

import java.util.List;

/** Шешім қабылдау қозғалтқышы: deny-overrides алгоритмі. */
public final class PolicyEngine {
    private final List<Policy> policies;

    public PolicyEngine(List<Policy> policies) {
        this.policies = List.copyOf(policies);
    }

    public Decision evaluate(AttributeContext ctx) {
        Policy firstPermit = null;

        for (Policy p : policies) {
            Effect result = p.evaluate(ctx);
            if (result == Effect.DENY) {
                // кем дегенде бір DENY сәйкес келсе, бірден тыйым
                return new Decision(Effect.DENY, p.getId(), "Тыйым салу саясаты сәйкес келді: " + p.getDescription());
            }
            if (result == Effect.PERMIT && firstPermit == null) {
                firstPermit = p;
            }
        }

        if (firstPermit != null) {
            return new Decision(Effect.PERMIT, firstPermit.getId(),
                    "Рұқсат саясаты сәйкес келді: " + firstPermit.getDescription());
        }
        return new Decision(Effect.DENY, null, "Ешбір саясат сәйкес келмеді (әдепкі тыйым)");
    }
}
