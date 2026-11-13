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
    private final String value;          // eq, neq
    private final List<String> values;   // in, not_in
    private final LocalTime from;        // time_between
    private final LocalTime to;

    public ConditionNode(String attribute, Operator operator, String value, List<String> values) {
        if (attribute == null || attribute.isBlank()) {
            throw new PolicyException("Шартта 'attribute' көрсетілмеген");
        }
        if (operator == null) {
            throw new PolicyException("Шартта 'operator' көрсетілмеген");
        }
        this.attribute = attribute;
        this.operator = operator;

        switch (operator) {
            case EQ, NEQ -> {
                if (value == null) {
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
    public boolean evaluate(AttributeContext ctx) {
        Optional<String> actual = ctx.get(attribute);
        if (actual.isEmpty()) {
            return false; // атрибут жоқ -> шарт орындалмайды (қауіпсіз жағы)
        }
        String a = actual.get();
        return switch (operator) {
            case EQ -> a.equals(value);
            case NEQ -> !a.equals(value);
            case IN -> values.contains(a);
            case NOT_IN -> !values.contains(a);
            case TIME_BETWEEN -> inTimeRange(a);
        };
    }

    /** [from, to) аралығы; from > to болса, түн арқылы өтеді (22:00–06:00). */
    private boolean inTimeRange(String actual) {
        LocalTime t;
        try {
            t = parseTime(actual);
        } catch (PolicyException e) {
            return false; // сұраныстағы уақыт форматы қате -> шарт орындалмайды
        }
        if (from.isBefore(to)) {
            return !t.isBefore(from) && t.isBefore(to);
        }
        return !t.isBefore(from) || t.isBefore(to);
    }

    static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim(), HH_MM);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new PolicyException("Уақыт форматы қате: '" + text + "'. Тек HH:mm (мысалы, 08:30) қолданылады");
        }
    }
}
