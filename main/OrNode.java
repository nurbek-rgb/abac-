package kz.abac;

import java.util.List;

/** Кем дегенде бір еншілес түйін шын болса, шын. */
public final class OrNode implements RuleNode {
    private final List<RuleNode> children;

    public OrNode(List<RuleNode> children) {
        if (children == null || children.isEmpty()) {
            throw new PolicyException("'or' түйінінде кемінде бір шарт болуы керек");
        }
        this.children = List.copyOf(children);
    }

    @Override
    public MatchResult evaluateResult(AttributeContext ctx) {
        boolean unresolved = false;
        for (RuleNode child : children) {
            MatchResult result = child.evaluateResult(ctx);
            if (result == MatchResult.TRUE) return MatchResult.TRUE;
            if (result == MatchResult.INDETERMINATE) unresolved = true;
        }
        return unresolved ? MatchResult.INDETERMINATE : MatchResult.FALSE;
    }
}

