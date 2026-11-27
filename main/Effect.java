package kz.abac;

/** Саясаттың немесе шешімнің әсері. */
public enum Effect {
    PERMIT,
    DENY,
    /** Саясаттың шарты сұранысқа сәйкес келмеді. */
    NOT_APPLICABLE,
    /** Internal policy result; the engine always returns PERMIT or DENY. */
    INDETERMINATE
}

