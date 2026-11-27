package kz.abac;

import java.util.List;

/** Барлық еншілес түйіндер шын болса, шын. */
public final class AndNode implements RuleNode {
    private final List<RuleNode> children;

    public AndNode(List<RuleNode> children) {
        if (children == null || children.isEmpty()) {
            throw new PolicyException("'and' түйінінде кемінде бір шарт болуы керек");
        }
        this.children = List.copyOf(children);
    }

    @Override
    public MatchResult evaluateResult(AttributeContext ctx) {
        boolean unresolved = false;
        for (RuleNode child : children) {
            MatchResult result = child.evaluateResult(ctx);
            if (result == MatchResult.FALSE) return MatchResult.FALSE;
            if (result == MatchResult.INDETERMINATE) unresolved = true;
        }
        return unresolved ? MatchResult.INDETERMINATE : MatchResult.TRUE;
    }
}

