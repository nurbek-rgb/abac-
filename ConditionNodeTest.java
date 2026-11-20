package kz.abac;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import kz.abac.ConditionNode.Operator;
import org.junit.jupiter.api.Test;

class ConditionNodeTest {

    private static AttributeContext ctx(String role, String time) {
        return AttributeContext.builder().role(role).time(time).build();
    }

    @Test
    void eqMatchesEqualValue() {
        var c = new ConditionNode("role", Operator.EQ, "doctor", null);
        assertTrue(c.evaluate(ctx("doctor", "10:00")));
        assertFalse(c.evaluate(ctx("nurse", "10:00")));
    }

    @Test
    void neqMatchesDifferentValue() {
        var c = new ConditionNode("role", Operator.NEQ, "admin", null);
        assertTrue(c.evaluate(ctx("doctor", "10:00")));
        assertFalse(c.evaluate(ctx("admin", "10:00")));
    }

    @Test
    void inAndNotInWorkWithLists() {
        var in = new ConditionNode("role", Operator.IN, null, List.of("doctor", "nurse"));
        var notIn = new ConditionNode("role", Operator.NOT_IN, null, List.of("guest"));
        assertTrue(in.evaluate(ctx("nurse", "10:00")));
        assertFalse(in.evaluate(ctx("guest", "10:00")));
        assertTrue(notIn.evaluate(ctx("doctor", "10:00")));
        assertFalse(notIn.evaluate(ctx("guest", "10:00")));
    }

    @Test
    void missingAttributeMakesEveryOperatorFalse() {
        var empty = AttributeContext.builder().build();
        assertFalse(new ConditionNode("role", Operator.EQ, "doctor", null).evaluate(empty));
        assertFalse(new ConditionNode("role", Operator.NEQ, "doctor", null).evaluate(empty));
        assertFalse(new ConditionNode("role", Operator.NOT_IN, null, List.of("guest")).evaluate(empty));
        assertFalse(new ConditionNode("time", Operator.TIME_BETWEEN, null, List.of("08:00", "18:00")).evaluate(empty));
    }

    @Test
    void timeBetweenDaytimeRange() {
        var c = new ConditionNode("time", Operator.TIME_BETWEEN, null, List.of("08:00", "18:00"));
        assertTrue(c.evaluate(ctx("x", "08:00")));   // бастамасы кіреді
        assertTrue(c.evaluate(ctx("x", "12:30")));
        assertFalse(c.evaluate(ctx("x", "18:00")));  // соңы кірмейді
        assertFalse(c.evaluate(ctx("x", "07:59")));
    }

    @Test
    void timeBetweenOvernightRange() {
        var c = new ConditionNode("time", Operator.TIME_BETWEEN, null, List.of("22:00", "06:00"));
        assertTrue(c.evaluate(ctx("x", "23:30")));
        assertTrue(c.evaluate(ctx("x", "00:00")));
        assertTrue(c.evaluate(ctx("x", "05:59")));
        assertFalse(c.evaluate(ctx("x", "06:00")));
        assertFalse(c.evaluate(ctx("x", "12:00")));
        assertFalse(c.evaluate(ctx("x", "21:59")));
    }

    @Test
    void invalidRequestTimeIsNotMatched() {
        var c = new ConditionNode("time", Operator.TIME_BETWEEN, null, List.of("08:00", "18:00"));
        assertFalse(c.evaluate(ctx("x", "25:00")));
        assertFalse(c.evaluate(ctx("x", "кеш")));
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThrows(PolicyException.class, () -> new ConditionNode("role", Operator.EQ, null, null));
        assertThrows(PolicyException.class, () -> new ConditionNode("role", Operator.IN, null, List.of()));
        assertThrows(PolicyException.class,
                () -> new ConditionNode("time", Operator.TIME_BETWEEN, null, List.of("8:00", "18:00")));
        assertThrows(PolicyException.class,
                () -> new ConditionNode("time", Operator.TIME_BETWEEN, null, List.of("08:00", "08:00")));
        assertThrows(PolicyException.class, () -> Operator.fromJson("gt"));
    }
}
