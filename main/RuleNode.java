package kz.abac;

/** Ереже ағашының түйіні (Composite үлгісі). */
public interface RuleNode {
    /** Internal evaluation must preserve missing/invalid data through all nested nodes. */
    MatchResult evaluateResult(AttributeContext ctx);

    /** Compatibility facade for callers: only a proven TRUE matches. */
    default boolean evaluate(AttributeContext ctx) {
        return evaluateResult(ctx) == MatchResult.TRUE;
    }
}

