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
    public boolean evaluate(AttributeContext ctx) {
        for (RuleNode child : children) {
            if (child.evaluate(ctx)) {
                return true;
            }
        }
        return false;
    }
}
