package kz.abac;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Written before the fix; also compiles against the original boolean API. */
class MissingAttributeSecurityTest {
    private static RuleNode eq(String key, String value) {
        return new ConditionNode(key, ConditionNode.Operator.EQ, value, null);
    }
    private static RuleNode notDevice() { return new NotNode(eq("device", "unknown")); }
    private static Policy permit(RuleNode node) { return new Policy("P", "permit", Effect.PERMIT, node); }
    private static AttributeContext doctor() { return AttributeContext.builder().role("doctor").build(); }

    @Test void missingAttributeUnderNotDoesNotMatch() {
        assertFalse(notDevice().evaluate(doctor()));
    }
    @ParameterizedTest @ValueSource(strings={"", " ", "\t"})
    void blankAttributeUnderNotDoesNotMatch(String value) {
        assertFalse(notDevice().evaluate(AttributeContext.builder().device(value).build()));
    }
    @Test void nullAttributeUnderNotDoesNotMatch() {
        assertFalse(notDevice().evaluate(AttributeContext.builder().device(null).build()));
    }
    @Test void nestedAndOrNotDoesNotGrant() {
        RuleNode tree = new AndNode(List.of(new OrNode(List.of(eq("role","doctor"),eq("role","nurse"))),notDevice()));
        assertFalse(tree.evaluate(doctor()));
    }
    @Test void notAroundOrDoesNotInvertMissingToTrue() {
        assertFalse(new NotNode(new OrNode(List.of(eq("role","nurse"),eq("device","unknown")))).evaluate(doctor()));
    }
    @Test void notAroundAndDoesNotInvertUnresolvedToTrue() {
        assertFalse(new NotNode(new AndNode(List.of(eq("role","doctor"),eq("device","unknown")))).evaluate(doctor()));
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4,5,6})
    void repeatedNotCannotCreatePermission(int depth) {
        RuleNode node=eq("device","unknown");
        for(int i=0;i<depth;i++) node=new NotNode(node);
        assertFalse(node.evaluate(doctor()));
    }
    @Test void onlyPermitPolicyWithMissingDeviceDenies() {
        assertEquals(Effect.DENY,new PolicyEngine(List.of(permit(notDevice()))).evaluate(doctor()).effect());
    }
    @Test void nonMatchingDenyDoesNotRescueUnsafePermit() {
        var deny=new Policy("D","guest denied",Effect.DENY,eq("role","guest"));
        assertEquals(Effect.DENY,new PolicyEngine(List.of(permit(notDevice()),deny)).evaluate(doctor()).effect());
    }
    @Test void unresolvedDenyCannotBeBypassedByKnownPermit() {
        var deny=new Policy("D","unknown device denied",Effect.DENY,eq("device","unknown"));
        for(var policies:List.of(List.of(permit(eq("role","doctor")),deny),List.of(deny,permit(eq("role","doctor"))))) {
            assertEquals(Effect.DENY,new PolicyEngine(policies).evaluate(doctor()).effect());
        }
    }
    @Test void malformedTimeUnderNotDoesNotGrant() {
        RuleNode time=new NotNode(new ConditionNode("time",ConditionNode.Operator.TIME_BETWEEN,null,List.of("08:00","18:00")));
        assertEquals(Effect.DENY,new PolicyEngine(List.of(permit(time))).evaluate(AttributeContext.builder().time("25:00").build()).effect());
    }
    @Test void knownSafeDeviceStillPermits() {
        assertTrue(new PolicyEngine(List.of(permit(notDevice()))).evaluate(AttributeContext.builder().device("corporate").build()).isPermit());
    }
    @Test void knownBlockedDeviceStillDenies() {
        assertFalse(new PolicyEngine(List.of(permit(notDevice()))).evaluate(AttributeContext.builder().device("unknown").build()).isPermit());
    }
    @Test void independentlyProvenOrBranchStillPermits() {
        assertTrue(new PolicyEngine(List.of(permit(new OrNode(List.of(eq("role","doctor"),notDevice()))))).evaluate(doctor()).isPermit());
    }
    @Test void missingValueDoesNotChangeKnownFalseAnd() {
        // NOT(FALSE AND unknown) is logically proven true: no missing data is used as evidence.
        assertTrue(new NotNode(new AndNode(List.of(eq("role","nurse"),eq("device","unknown")))).evaluate(doctor()));
    }
}
