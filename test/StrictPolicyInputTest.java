package kz.abac;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StrictPolicyInputTest {
    private static String policy(String condition) {
        return "{\"policies\":[{\"id\":\"P\",\"effect\":\"PERMIT\",\"condition\":"+condition+"}]}";
    }
    @ParameterizedTest @ValueSource(strings={
        "{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":{}}",
        "{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":null}",
        "{\"attribute\":\"a\",\"operator\":\"in\",\"values\":[null]}",
        "{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":\"a\",\"values\":[\"a\"]}",
        "{\"attribute\":\"a\",\"operator\":\"in\",\"values\":[\"a\"],\"valueAttribute\":\"b\"}",
        "{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":\"a\",\"typo\":true}"})
    void invalidLeafRejected(String leaf) { assertThrows(PolicyException.class,()->PolicyLoader.parse(policy(leaf))); }
    @Test void duplicateKeysAndTrailingJsonRejected() {
        assertThrows(PolicyException.class,()->PolicyLoader.parse("{\"policies\":[],\"policies\":[]}"));
        assertThrows(PolicyException.class,()->PolicyLoader.parse("{\"policies\":[]} {}"));
        assertThrows(PolicyException.class,()->PolicyLoader.parse(null));
    }
    @Test void jsonLoadedNotCannotGrantOnMissingAttribute() {
        var engine=new PolicyEngine(PolicyLoader.parse(policy("{\"not\":{\"attribute\":\"device\",\"operator\":\"eq\",\"value\":\"unknown\"}}")));
        assertFalse(engine.evaluate(AttributeContext.builder().build()).isPermit());
        assertTrue(engine.evaluate(AttributeContext.builder().device("corporate").build()).isPermit());
    }
    @Test void recursiveTreeLimitIsExplicit() {
        String node="{\"attribute\":\"a\",\"operator\":\"eq\",\"value\":\"x\"}";
        for(int i=0;i<130;i++) node="{\"not\":"+node+"}";
        String json=policy(node);
        assertThrows(PolicyException.class,()->PolicyLoader.parse(json));
    }
    @Test void requestFileParsesScalarsAndRejectsNestedObjects(@TempDir Path dir) throws Exception {
        Path file=dir.resolve("requests.json");Files.writeString(file,"[{\"role\":\"doctor\",\"emergency\":true,\"n\":2,\"device\":null}]");
        var row=RequestLoader.load(file).get(0);
        assertEquals("true",row.get("emergency").orElseThrow());assertTrue(row.get("device").isEmpty());
        Files.writeString(file,"[{\"role\":{}}]");assertThrows(PolicyException.class,()->RequestLoader.load(file));
    }
}
