package kz.abac;

import java.util.Objects;

/** Саясат: id, сипаттама, әсері (PERMIT/DENY) және шарттар ағашы. */
public final class Policy {
    private final String id;
    private final String description;
    private final Effect effect;
    private final RuleNode condition;

    public Policy(String id, String description, Effect effect, RuleNode condition) {
        if (id == null || id.isBlank()) {
            throw new PolicyException("Саясатта 'id' жоқ");
        }
        if (effect != Effect.PERMIT && effect != Effect.DENY) {
            throw new PolicyException("Саясат '" + id + "': effect тек PERMIT немесе DENY болуы керек");
        }
        this.id = id;
        this.description = description == null ? "" : description;
        this.effect = effect;
        this.condition = Objects.requireNonNull(condition, "condition");
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public Effect getEffect() { return effect; }

    /** Distinguishes a false condition from one that cannot be evaluated. */
    public Effect evaluate(AttributeContext ctx) {
        return switch (condition.evaluateResult(ctx)) {
            case TRUE -> effect;
            case FALSE -> Effect.NOT_APPLICABLE;
            case INDETERMINATE -> Effect.INDETERMINATE;
        };
    }
}

