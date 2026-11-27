package kz.abac;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyLoaderTest {

    private static String policy(String condition) {
        return """
            {"policies":[{"id":"P1","description":"t","effect":"PERMIT","condition":%s}]}
            """.formatted(condition);
    }

    @Test
    void loadsSamplePoliciesFile() {
        List<Policy> list = PolicyLoader.load(Path.of("policies.json"));
        assertEquals(4, list.size());

        var engine = new PolicyEngine(list);
        var doctor = AttributeContext.builder().role("doctor").time("10:00").device("corporate")
                .location("office").action("read").build();
        assertEquals(Effect.PERMIT, engine.evaluate(doctor).effect());

        var adminAtNight = AttributeContext.builder().role("admin").time("23:30").device("corporate")
                .location("unknown").action("read").build();
        Decision d = engine.evaluate(adminAtNight);
        assertEquals(Effect.DENY, d.effect());
        assertEquals("P3", d.policyId());
    }

    @Test
    void buildsDeepTree() {
        String json = policy("""
            {"and":[{"or":[{"attribute":"role","operator":"eq","value":"a"},
                           {"not":{"attribute":"role","operator":"in","values":["b","c"]}}]},
                    {"attribute":"action","operator":"eq","value":"read"}]}
            """);
        var engine = new PolicyEngine(PolicyLoader.parse(json));
        var ctx = AttributeContext.builder().role("zzz").action("read").build();
        assertEquals(Effect.PERMIT, engine.evaluate(ctx).effect());
    }

    @Test
    void rejectsBrokenJson() {
        var e = assertThrows(PolicyException.class, () -> PolicyLoader.parse("{ policies: "));
        assertTrue(e.getMessage().startsWith("Жарамсыз JSON"));
    }

    @Test
    void rejectsMissingPoliciesArray() {
        assertThrows(PolicyException.class, () -> PolicyLoader.parse("{}"));
    }

    @Test
    void rejectsUnknownOperatorWithPath() {
        var e = assertThrows(PolicyException.class, () -> PolicyLoader.parse(
                policy("{\"attribute\":\"age\",\"operator\":\"gt\",\"value\":\"18\"}")));
        assertTrue(e.getMessage().contains("gt"));
        assertTrue(e.getMessage().contains("P1"));
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThrows(PolicyException.class, () -> PolicyLoader.parse(
                "{\"policies\":[{\"effect\":\"PERMIT\",\"condition\":{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":\"b\"}}]}"));
        assertThrows(PolicyException.class, () -> PolicyLoader.parse(
                "{\"policies\":[{\"id\":\"P1\",\"effect\":\"PERMIT\"}]}"));
        assertThrows(PolicyException.class, () -> PolicyLoader.parse(
                "{\"policies\":[{\"id\":\"P1\",\"effect\":\"MAYBE\",\"condition\":{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":\"b\"}}]}"));
    }

    @Test
    void rejectsAmbiguousOrEmptyNodes() {
        assertThrows(PolicyException.class, () -> PolicyLoader.parse(policy("{}")));
        assertThrows(PolicyException.class, () -> PolicyLoader.parse(policy("{\"and\":[]}")));
        assertThrows(PolicyException.class, () -> PolicyLoader.parse(policy(
                "{\"and\":[{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":\"b\"}],\"or\":[]}")));
    }

    @Test
    void rejectsDuplicateIds() {
        String leaf = "{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":\"b\"}";
        String json = "{\"policies\":["
                + "{\"id\":\"P1\",\"effect\":\"PERMIT\",\"condition\":" + leaf + "},"
                + "{\"id\":\"P1\",\"effect\":\"DENY\",\"condition\":" + leaf + "}]}";
        assertThrows(PolicyException.class, () -> PolicyLoader.parse(json));
    }

    @Test
    void rejectsBadTimeFormatInPolicy() {
        assertThrows(PolicyException.class, () -> PolicyLoader.parse(policy(
                "{\"attribute\":\"time\",\"operator\":\"time_between\",\"values\":[\"8:00\",\"18:00\"]}")));
    }

    @Test
    void missingFileGivesClearError() {
        assertThrows(PolicyException.class, () -> PolicyLoader.load(Path.of("nope-does-not-exist.json")));
    }
}

