package kz.abac;

/** Strong Kleene three-valued logic; unknown must never be negated into true. */
public enum MatchResult {
    TRUE, FALSE, INDETERMINATE;

    public static MatchResult of(boolean value) { return value ? TRUE : FALSE; }

    public MatchResult not() {
        return switch (this) {
            case TRUE -> FALSE;
            case FALSE -> TRUE;
            case INDETERMINATE -> INDETERMINATE;
        };
    }
}
