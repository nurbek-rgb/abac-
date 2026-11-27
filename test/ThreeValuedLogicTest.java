package kz.abac;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ThreeValuedLogicTest {
    private static final AttributeContext EMPTY=AttributeContext.builder().build();
    private static RuleNode constant(MatchResult value) { return ctx->value; }
    @ParameterizedTest
    @CsvSource({"TRUE,TRUE,TRUE,TRUE", "TRUE,FALSE,FALSE,TRUE", "TRUE,INDETERMINATE,INDETERMINATE,TRUE",
        "FALSE,TRUE,FALSE,TRUE", "FALSE,FALSE,FALSE,FALSE", "FALSE,INDETERMINATE,FALSE,INDETERMINATE",
        "INDETERMINATE,TRUE,INDETERMINATE,TRUE", "INDETERMINATE,FALSE,FALSE,INDETERMINATE", "INDETERMINATE,INDETERMINATE,INDETERMINATE,INDETERMINATE"})
    void fullTruthTables(MatchResult a,MatchResult b,MatchResult and,MatchResult or) {
        assertEquals(and,new AndNode(List.of(constant(a),constant(b))).evaluateResult(EMPTY));
        assertEquals(or,new OrNode(List.of(constant(a),constant(b))).evaluateResult(EMPTY));
        assertEquals(and.not(),new NotNode(new AndNode(List.of(constant(a),constant(b)))).evaluateResult(EMPTY));
        assertEquals(or.not(),new NotNode(new OrNode(List.of(constant(a),constant(b)))).evaluateResult(EMPTY));
    }
    @Test void unresolvedPermitDoesNotBlockIndependentProof() {
        var policies=List.of(new Policy("unknown","",Effect.PERMIT,constant(MatchResult.INDETERMINATE)),
                new Policy("known","",Effect.PERMIT,constant(MatchResult.TRUE)));
        assertTrue(new PolicyEngine(policies).evaluate(EMPTY).isPermit());
    }
    @Test void explicitDenyHasPriorityOverUnresolvedDeny() {
        var policies=List.of(new Policy("unknown","",Effect.DENY,constant(MatchResult.INDETERMINATE)),
                new Policy("known","",Effect.DENY,constant(MatchResult.TRUE)));
        assertEquals("known",new PolicyEngine(policies).evaluate(EMPTY).policyId());
    }
    @Test void missingReferencedAttributeRemainsUnknownUnderNot() {
        var leaf=new ConditionNode("a",ConditionNode.Operator.NEQ,null,"b",null);
        var ctx=AttributeContext.builder().attribute("a","x").build();
        assertEquals(MatchResult.INDETERMINATE,new NotNode(leaf).evaluateResult(ctx));
        assertFalse(new NotNode(new NotNode(leaf)).evaluate(ctx));
    }
    @Test void negationOfAllOperatorsHandlesMissingData() {
        var leaves=List.of(new ConditionNode("x",ConditionNode.Operator.EQ,"a",null),
            new ConditionNode("x",ConditionNode.Operator.NEQ,"a",null),
            new ConditionNode("x",ConditionNode.Operator.IN,null,List.of("a")),
            new ConditionNode("x",ConditionNode.Operator.NOT_IN,null,List.of("a")),
            new ConditionNode("x",ConditionNode.Operator.TIME_BETWEEN,null,List.of("08:00","18:00")));
        for(var leaf:leaves) assertEquals(MatchResult.INDETERMINATE,new NotNode(leaf).evaluateResult(EMPTY));
    }
    @Test void ruleFailureAndNullRequestCannotReturnPermit() {
        var failing=new Policy("failure","",Effect.PERMIT,ctx->{throw new IllegalStateException("failure");});
        assertFalse(new PolicyEngine(List.of(failing)).evaluate(EMPTY).isPermit());
        assertFalse(new PolicyEngine(List.of(failing)).evaluate(null).isPermit());
    }
    @Test void builderNullClearsPreviouslySetValue() {
        assertTrue(AttributeContext.builder().device("corporate").device(null).build().get("device").isEmpty());
    }
    @Test void immutableContextDoesNotFollowLaterBuilderChanges() {
        var builder=AttributeContext.builder().role("doctor"); var ctx=builder.build();builder.role("nurse");
        assertEquals("doctor",ctx.get("role").orElseThrow());
        assertThrows(UnsupportedOperationException.class,()->ctx.attributes().put("role","admin"));
    }
}
