package kz.abac;

/** Шешім: әсері, себеп болған саясат id-і (әдепкі DENY үшін null) және түсіндірме. */
public record Decision(Effect effect, String policyId, String reason) {

    public boolean isPermit() {
        return effect == Effect.PERMIT;
    }

    @Override
    public String toString() {
        return effect + (policyId == null ? "" : " [" + policyId + "]") + " — " + reason;
    }
}

