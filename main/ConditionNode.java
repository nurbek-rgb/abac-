package kz.abac;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Optional;

/** Жапырақ шарт: атрибут + оператор + мән(дер). */
public final class ConditionNode implements RuleNode {

    /** Қолдау көрсетілетін операторлар. */
    public enum Operator {
        EQ("eq"), NEQ("neq"), IN("in"), NOT_IN("not_in"), TIME_BETWEEN("time_between");

        private final String json;

        Operator(String json) {
            this.json = json;
        }

        public String json() {
            return json;
        }

        public static Operator fromJson(String name) {
            for (Operator op : values()) {
                if (op.json.equals(name)) {
                    return op;
                }
            }
            throw new PolicyException("Белгісіз оператор: '" + name
                    + "'. Рұқсат етілгені: eq, neq, in, not_in, time_between");
        }
    }

    private static final DateTimeFormatter HH_MM =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final String attribute;
    private final Operator operator;
    private final String valueAttribute;
    private final String value;          // eq, neq
    private final List<String> values;   // in, not_in
    private final LocalTime from;        // time_between
    private final LocalTime to;

    public ConditionNode(String attribute, Operator operator, String value, List<String> values) {
        this(attribute, operator, value, null, values);
    }

    public ConditionNode(String attribute, Operator operator, String value, String valueAttribute, List<String> values) {
        if (attribute == null || attribute.isBlank()) {
            throw new PolicyException("Шартта 'attribute' көрсетілмеген");
        }
        if (operator == null) {
            throw new PolicyException("Шартта 'operator' көрсетілмеген");
        }
        this.attribute = attribute;
        this.operator = operator;
        this.valueAttribute = valueAttribute;
        if (valueAttribute != null && valueAttribute.isBlank()) throw new PolicyException("valueAttribute is blank");
        if (operator != Operator.EQ && operator != Operator.NEQ && (value != null || valueAttribute != null)) {
            throw new PolicyException("Only eq/neq support value or valueAttribute");
        }

        switch (operator) {
            case EQ, NEQ -> {
                if ((value == null) == (valueAttribute == null) || values != null) {
                    throw new PolicyException("'" + operator.json() + "' операторына 'value' қажет (атрибут: "
                            + attribute + ")");
                }
                this.value = value;
                this.values = List.of();
                this.from = null;
                this.to = null;
            }
            case IN, NOT_IN -> {
                if (values == null || values.isEmpty()) {
                    throw new PolicyException("'" + operator.json() + "' операторына бос емес 'values' тізімі қажет"
                            + " (атрибут: " + attribute + ")");
                }
                this.value = null;
                this.values = List.copyOf(values);
                this.from = null;
                this.to = null;
            }
            case TIME_BETWEEN -> {
                if (values == null || values.size() != 2) {
                    throw new PolicyException("'time_between' операторына 'values': [\"HH:mm\", \"HH:mm\"] қажет");
                }
                this.value = null;
                this.values = List.of();
                this.from = parseTime(values.get(0));
                this.to = parseTime(values.get(1));
                if (this.from.equals(this.to)) {
                    throw new PolicyException("'time_between' бастапқы және соңғы уақыты бірдей болмауы керек: "
                            + values.get(0));
                }
            }
            default -> throw new IllegalStateException();
        }
    }

    @Override
    public MatchResult evaluateResult(AttributeContext ctx) {
        Optional<String> actual = ctx.get(attribute);
        if (actual.isEmpty()) {
            return MatchResult.INDETERMINATE;
        }
        String a = actual.get();
        String expected = value;
        if (valueAttribute != null) {
            Optional<String> referenced = ctx.get(valueAttribute);
            if (referenced.isEmpty()) return MatchResult.INDETERMINATE;
            expected = referenced.get();
        }
        return switch (operator) {
            case EQ -> MatchResult.of(a.equals(expected));
            case NEQ -> MatchResult.of(!a.equals(expected));
            case IN -> MatchResult.of(values.contains(a));
            case NOT_IN -> MatchResult.of(!values.contains(a));
            case TIME_BETWEEN -> inTimeRange(a);
        };
    }

    /** [from, to) аралығы; from > to болса, түн арқылы өтеді (22:00–06:00). */
    private MatchResult inTimeRange(String actual) {
        LocalTime t;
        try {
            t = parseTime(actual);
        } catch (PolicyException e) {
            return MatchResult.INDETERMINATE; // invalid input is not evidence for NOT
        }
        if (from.isBefore(to)) {
            return MatchResult.of(!t.isBefore(from) && t.isBefore(to));
        }
        return MatchResult.of(!t.isBefore(from) || t.isBefore(to));
    }

    static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim(), HH_MM);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new PolicyException("Уақыт форматы қате: '" + text + "'. Тек HH:mm (мысалы, 08:30) қолданылады");
        }
    }
}

