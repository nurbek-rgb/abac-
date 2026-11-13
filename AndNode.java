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
    public boolean evaluate(AttributeContext ctx) {
        for (RuleNode child : children) {
            if (!child.evaluate(ctx)) {
                return false;
            }
        }
        return true;
    }
}
