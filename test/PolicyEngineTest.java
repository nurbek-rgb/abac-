package kz.abac;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import kz.abac.ConditionNode.Operator;
import org.junit.jupiter.api.Test;

class PolicyEngineTest {

    private static RuleNode role(String r) {
        return new ConditionNode("role", Operator.EQ, r, null);
    }

    private static AttributeContext ctxWithRole(String r) {
        return AttributeContext.builder().role(r).build();
    }

    @Test
    void andOrNotCompose() {
        RuleNode tree = new AndNode(List.of(
                new OrNode(List.of(role("doctor"), role("nurse"))),
                new NotNode(new ConditionNode("device", Operator.EQ, "unknown", null))));

        var ok = AttributeContext.builder().role("nurse").device("corporate").build();
        var badDevice = AttributeContext.builder().role("nurse").device("unknown").build();
        var badRole = AttributeContext.builder().role("guest").device("corporate").build();
        assertTrue(tree.evaluate(ok));
        assertFalse(tree.evaluate(badDevice));
        assertFalse(tree.evaluate(badRole));
    }

    @Test
    void permitWhenOnlyPermitMatches() {
        var engine = new PolicyEngine(List.of(new Policy("P1", "d", Effect.PERMIT, role("doctor"))));
        Decision d = engine.evaluate(ctxWithRole("doctor"));
        assertEquals(Effect.PERMIT, d.effect());
        assertEquals("P1", d.policyId());
    }

    @Test
    void denyOverridesPermit() {
        var engine = new PolicyEngine(List.of(
                new Policy("P1", "permit", Effect.PERMIT, role("doctor")),
                new Policy("P2", "deny", Effect.DENY, role("doctor"))));
        Decision d = engine.evaluate(ctxWithRole("doctor"));
        assertEquals(Effect.DENY, d.effect());
        assertEquals("P2", d.policyId());
    }

    @Test
    void denyWinsRegardlessOfOrder() {
        var engine = new PolicyEngine(List.of(
                new Policy("P2", "deny", Effect.DENY, role("doctor")),
                new Policy("P1", "permit", Effect.PERMIT, role("doctor"))));
        assertEquals(Effect.DENY, engine.evaluate(ctxWithRole("doctor")).effect());
    }

    @Test
    void defaultDenyWhenNothingMatches() {
        var engine = new PolicyEngine(List.of(new Policy("P1", "d", Effect.PERMIT, role("doctor"))));
        Decision d = engine.evaluate(ctxWithRole("guest"));
        assertEquals(Effect.DENY, d.effect());
        assertNull(d.policyId());
    }

    @Test
    void emptyPolicyListDenies() {
        assertEquals(Effect.DENY, new PolicyEngine(List.of()).evaluate(ctxWithRole("doctor")).effect());
    }
}

