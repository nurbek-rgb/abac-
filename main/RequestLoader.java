package kz.abac;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads a JSON array of request objects; malformed data is never silently discarded. */
public final class RequestLoader {
    private static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private RequestLoader() {}
    public static List<AttributeContext> load(Path file) {
        try {
            JsonNode root=JSON.readTree(Files.readString(file));
            if(root==null || !root.isArray()) throw new PolicyException("Requests must be a JSON array");
            List<AttributeContext> requests=new ArrayList<>();
            for(JsonNode row:root) {
                if(!row.isObject()) throw new PolicyException("Each request must be an object");
                AttributeContext.Builder builder=AttributeContext.builder();
                row.fields().forEachRemaining(entry->{
                    JsonNode value=entry.getValue();
                    if(value.isNull()) builder.attribute(entry.getKey(),null);
                    else if(value.isTextual() || value.isBoolean() || value.isNumber()) builder.attribute(entry.getKey(),value.asText());
                    else throw new PolicyException("Request attributes must be scalar values");
                });
                requests.add(builder.build());
            }
            return List.copyOf(requests);
        } catch(IOException e) { throw new PolicyException("Cannot read requests: "+file,e); }
    }
}
