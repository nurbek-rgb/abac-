package kz.abac;

import java.util.List;

/** Шешім қабылдау қозғалтқышы: deny-overrides алгоритмі. */
public final class PolicyEngine {
    private final List<Policy> policies;

    public PolicyEngine(List<Policy> policies) {
        this.policies = List.copyOf(policies);
    }

    public Decision evaluate(AttributeContext ctx) {
        if (ctx == null) return new Decision(Effect.DENY, null, "Сұраныс жоқ (fail-closed)");
        Policy firstPermit = null;
        Policy unresolvedDeny = null;
        Policy unresolvedPermit = null;

        for (Policy p : policies) {
            Effect result;
            try {
                result = p.evaluate(ctx);
            } catch (RuntimeException e) {
                return new Decision(Effect.DENY, p.getId(), "Саясатты бағалау қатесі (fail-closed)");
            }
            if (result == Effect.DENY) {
                // кем дегенде бір DENY сәйкес келсе, бірден тыйым
                return new Decision(Effect.DENY, p.getId(), "Тыйым салу саясаты сәйкес келді: " + p.getDescription());
            }
            if (result == Effect.PERMIT && firstPermit == null) {
                firstPermit = p;
            }
            if (result == Effect.INDETERMINATE) {
                if (p.getEffect() == Effect.DENY && unresolvedDeny == null) unresolvedDeny = p;
                if (p.getEffect() == Effect.PERMIT && unresolvedPermit == null) unresolvedPermit = p;
            }
        }

        if (unresolvedDeny != null) {
            return new Decision(Effect.DENY, unresolvedDeny.getId(),
                    "DENY саясатын тексеруге дерек жеткіліксіз/жарамсыз (fail-closed)");
        }
        if (firstPermit != null) {
            return new Decision(Effect.PERMIT, firstPermit.getId(),
                    "Рұқсат саясаты сәйкес келді: " + firstPermit.getDescription());
        }
        if (unresolvedPermit != null) {
            return new Decision(Effect.DENY, unresolvedPermit.getId(),
                    "PERMIT шарты дәлелденбеді: дерек жеткіліксіз/жарамсыз (fail-closed)");
        }
        return new Decision(Effect.DENY, null, "Ешбір саясат сәйкес келмеді (әдепкі тыйым)");
    }
}

