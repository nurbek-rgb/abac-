package kz.abac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** policies.json файлын оқиды, тексереді және ереже ағашын құрады. */
public final class PolicyLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private PolicyLoader() {
    }

    public static List<Policy> load(Path file) {
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PolicyException("Саясат файлын оқу мүмкін болмады: " + file, e);
        }
        return parse(json);
    }

    public static List<Policy> parse(String json) {
        if (json == null || json.isBlank()) throw new PolicyException("Жарамсыз JSON: empty input");
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new PolicyException("Жарамсыз JSON: " + e.getOriginalMessage(), e);
        }
        if (root == null || !root.isObject() || !root.path("policies").isArray()) {
            throw new PolicyException("JSON түбірінде 'policies' массиві болуы керек");
        }

        checkFields(root, Set.of("policies"), "root");
        List<Policy> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        int index = 0;
        for (JsonNode p : root.get("policies")) {
            Policy policy = parsePolicy(p, index++);
            if (!ids.add(policy.getId())) {
                throw new PolicyException("Саясат id қайталанды: '" + policy.getId() + "'");
            }
            result.add(policy);
        }
        return result;
    }

    private static Policy parsePolicy(JsonNode p, int index) {
        String where = "policies[" + index + "]";
        if (!p.isObject()) {
            throw new PolicyException(where + ": саясат объект болуы керек");
        }
        checkFields(p, Set.of("id", "description", "effect", "condition"), where);
        String id = text(p, "id", where);
        where = "саясат '" + id + "'";

        String effectText = text(p, "effect", where);
        Effect effect;
        if ("PERMIT".equals(effectText)) {
            effect = Effect.PERMIT;
        } else if ("DENY".equals(effectText)) {
            effect = Effect.DENY;
        } else {
            throw new PolicyException(where + ": effect тек PERMIT немесе DENY болуы керек, берілгені: '"
                    + effectText + "'");
        }

        JsonNode cond = p.get("condition");
        if (cond == null || cond.isNull()) {
            throw new PolicyException(where + ": міндетті 'condition' өрісі жоқ");
        }
        String description = p.path("description").asText("");
        return new Policy(id, description, effect, parseNode(cond, where + ".condition", 0));
    }

    private static RuleNode parseNode(JsonNode n, String path, int depth) {
        if (depth > 128) throw new PolicyException(path + ": tree depth exceeds 128");
        if (!n.isObject()) {
            throw new PolicyException(path + ": шарт түйіні объект болуы керек");
        }
        boolean hasAnd = n.has("and");
        boolean hasOr = n.has("or");
        boolean hasNot = n.has("not");
        boolean isLeaf = n.has("attribute") || n.has("operator");

        int kinds = (hasAnd ? 1 : 0) + (hasOr ? 1 : 0) + (hasNot ? 1 : 0) + (isLeaf ? 1 : 0);
        if (kinds != 1) {
            throw new PolicyException(path + ": түйінде дәл біреуі болуы керек: and / or / not / "
                    + "attribute+operator");
        }

        if (hasAnd) {
            checkFields(n, Set.of("and"), path);
            return new AndNode(parseChildren(n.get("and"), path + ".and", depth + 1));
        }
        if (hasOr) {
            checkFields(n, Set.of("or"), path);
            return new OrNode(parseChildren(n.get("or"), path + ".or", depth + 1));
        }
        if (hasNot) {
            checkFields(n, Set.of("not"), path);
            return new NotNode(parseNode(n.get("not"), path + ".not", depth + 1));
        }
        return parseLeaf(n, path);
    }

    private static List<RuleNode> parseChildren(JsonNode arr, String path, int depth) {
        if (!arr.isArray() || arr.isEmpty()) {
            throw new PolicyException(path + ": бос емес массив болуы керек");
        }
        List<RuleNode> children = new ArrayList<>();
        int i = 0;
        for (JsonNode child : arr) {
            children.add(parseNode(child, path + "[" + i++ + "]", depth));
        }
        return children;
    }

    private static RuleNode parseLeaf(JsonNode n, String path) {
        try {
            checkFields(n, Set.of("attribute", "operator", "value", "values", "valueAttribute"), path);
            String attribute = text(n, "attribute", path);
            ConditionNode.Operator op = ConditionNode.Operator.fromJson(text(n, "operator", path));

            String value = n.has("value") ? scalar(n.get("value")) : null;
            String reference = n.has("valueAttribute") ? text(n, "valueAttribute", path) : null;
            List<String> values = null;
            if (n.has("values")) {
                if (!n.get("values").isArray()) {
                    throw new PolicyException("'values' массив болуы керек");
                }
                values = new ArrayList<>();
                for (JsonNode v : n.get("values")) {
                    values.add(scalar(v));
                }
            }
            return new ConditionNode(attribute, op, value, reference, values);
        } catch (PolicyException e) {
            throw new PolicyException(path + ": " + e.getMessage(), e);
        }
    }

    private static String scalar(JsonNode n) {
        if (n == null || n.isNull() || !(n.isTextual() || n.isNumber() || n.isBoolean())) {
            throw new PolicyException("value/values entries must be string, number or boolean");
        }
        return n.asText();
    }

    private static void checkFields(JsonNode node, Set<String> allowed, String path) {
        node.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) throw new PolicyException(path + ": unknown field '" + field + "'");
        });
    }

    private static String text(JsonNode n, String field, String where) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull() || !v.isTextual() || v.asText().isBlank()) {
            throw new PolicyException(where + ": міндетті '" + field + "' өрісі жоқ немесе бос");
        }
        return v.asText();
    }
}

