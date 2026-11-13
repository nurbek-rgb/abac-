package kz.abac;

/** Ереже ағашының түйіні (Composite үлгісі). */
public interface RuleNode {
    /** Түйін шарты сұраныс атрибуттарымен орындала ма. */
    boolean evaluate(AttributeContext ctx);
}
