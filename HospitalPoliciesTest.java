package kz.abac;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import kz.abac.ConditionNode.Operator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HospitalPoliciesTest {
    private static PolicyEngine engine;

    @BeforeAll
    static void load() {
        engine = new PolicyEngine(PolicyLoader.load(Path.of("policies-hospital.json")));
    }

    private static AttributeContext req(String role, String dept, String recordDept, String action,
                                        String time, boolean emergency) {
        return AttributeContext.builder().role(role).device("corporate").time(time).action(action)
                .attribute("department", dept).attribute("record_department", recordDept)
                .attribute("emergency", String.valueOf(emergency)).build();
    }

    @Test
    void doctorReadsOwnDepartment() {
        Decision d = engine.evaluate(req("doctor", "cardiology", "cardiology", "read", "10:00", false));
        assertEquals(Effect.PERMIT, d.effect());
        assertEquals("H1", d.policyId());
    }

    @Test
    void doctorCannotReadOtherDepartmentWithoutEmergency() {
        assertEquals(Effect.DENY,
                engine.evaluate(req("doctor", "cardiology", "neurology", "read", "10:00", false)).effect());
    }

    @Test
    void emergencyBreakGlassPermitsOtherDepartment() {
        Decision d = engine.evaluate(req("doctor", "cardiology", "neurology", "read", "03:00", true));
        assertEquals(Effect.PERMIT, d.effect());
        assertEquals("H3", d.policyId());
    }

    @Test
    void nurseOnlyDuringWorkHours() {
        assertTrue(engine.evaluate(req("nurse", "icu", "icu", "read", "14:00", false)).isPermit());
        assertFalse(engine.evaluate(req("nurse", "icu", "icu", "read", "23:00", false)).isPermit());
    }

    @Test
    void nurseCannotPrescribe() {
        Decision d = engine.evaluate(req("nurse", "icu", "icu", "prescribe", "10:00", false));
        assertEquals(Effect.DENY, d.effect());
        assertEquals("H4", d.policyId());
    }

    @Test
    void registrarIsDenied() {
        assertEquals("H5", engine.evaluate(req("registrar", "reception", "cardiology", "read", "10:00", false)).policyId());
    }

    @Test
    void missingRecordDepartmentFailsSafe() {
        var ctx = AttributeContext.builder().role("doctor").device("corporate").action("read")
                .attribute("department", "cardiology").build();
        assertFalse(engine.evaluate(ctx).isPermit());
    }

    @Test
    void attributeReferenceComparison() {
        var eq = new ConditionNode("a", Operator.EQ, null, "b", null);
        var neq = new ConditionNode("a", Operator.NEQ, null, "b", null);
        var same = AttributeContext.builder().attribute("a", "x").attribute("b", "x").build();
        var diff = AttributeContext.builder().attribute("a", "x").attribute("b", "y").build();
        assertTrue(eq.evaluate(same));
        assertFalse(eq.evaluate(diff));
        assertTrue(neq.evaluate(diff));
        assertFalse(neq.evaluate(AttributeContext.builder().attribute("a", "x").build())); // b жоқ
    }

    @Test
    void valueAndValueAttributeTogetherAreRejected() {
        assertThrows(PolicyException.class, () -> new ConditionNode("a", Operator.EQ, "x", "b", null));
        assertThrows(PolicyException.class, () -> new ConditionNode("a", Operator.EQ, null, null, null));
    }
}
