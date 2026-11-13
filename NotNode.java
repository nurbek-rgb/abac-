package kz.abac;

import java.util.Objects;

/**
 * Еншілес түйінді терістейді.
 * Ескерту: жоқ атрибутқа қатысты жапырақ шарт false қайтарады, ал NOT оны true-ға айналдырады.
 * Қауіпсіздік үшін "жоқ атрибут" жағдайын бөлек шартпен тексеріңіз.
 */
public final class NotNode implements RuleNode {
    private final RuleNode child;

    public NotNode(RuleNode child) {
        this.child = Objects.requireNonNull(child, "child");
    }

    @Override
    public boolean evaluate(AttributeContext ctx) {
        return !child.evaluate(ctx);
    }
}
