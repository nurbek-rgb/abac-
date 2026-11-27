package kz.abac;

import java.util.Objects;

/**
 * Еншілес түйінді терістейді.
 * Жоқ/жарамсыз дерек INDETERMINATE болып қалады; true-ға айналмайды.
 */
public final class NotNode implements RuleNode {
    private final RuleNode child;

    public NotNode(RuleNode child) {
        this.child = Objects.requireNonNull(child, "child");
    }

    @Override
    public MatchResult evaluateResult(AttributeContext ctx) {
        return child.evaluateResult(ctx).not();
    }
}

