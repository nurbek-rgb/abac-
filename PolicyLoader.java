package kz.abac;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new PolicyException("Жарамсыз JSON: " + e.getOriginalMessage(), e);
        }
        if (root == null || !root.isObject() || !root.path("policies").isArray()) {
            throw new PolicyException("JSON түбірінде 'policies' массиві болуы керек");
        }

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
        return new Policy(id, description, effect, parseNode(cond, where + ".condition"));
    }

    private static RuleNode parseNode(JsonNode n, String path) {
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
            return new AndNode(parseChildren(n.get("and"), path + ".and"));
        }
        if (hasOr) {
            return new OrNode(parseChildren(n.get("or"), path + ".or"));
        }
        if (hasNot) {
            return new NotNode(parseNode(n.get("not"), path + ".not"));
        }
        return parseLeaf(n, path);
    }

    private static List<RuleNode> parseChildren(JsonNode arr, String path) {
        if (!arr.isArray() || arr.isEmpty()) {
            throw new PolicyException(path + ": бос емес массив болуы керек");
        }
        List<RuleNode> children = new ArrayList<>();
        int i = 0;
        for (JsonNode child : arr) {
            children.add(parseNode(child, path + "[" + i++ + "]"));
        }
        return children;
    }

    private static RuleNode parseLeaf(JsonNode n, String path) {
        try {
            String attribute = text(n, "attribute", path);
            ConditionNode.Operator op = ConditionNode.Operator.fromJson(text(n, "operator", path));

            String value = n.hasNonNull("value") ? n.get("value").asText() : null;
            List<String> values = null;
            if (n.has("values")) {
                if (!n.get("values").isArray()) {
                    throw new PolicyException("'values' массив болуы керек");
                }
                values = new ArrayList<>();
                for (JsonNode v : n.get("values")) {
                    values.add(v.asText());
                }
            }
            return new ConditionNode(attribute, op, value, values);
        } catch (PolicyException e) {
            throw new PolicyException(path + ": " + e.getMessage(), e);
        }
    }

    private static String text(JsonNode n, String field, String where) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull() || !v.isTextual() || v.asText().isBlank()) {
            throw new PolicyException(where + ": міндетті '" + field + "' өрісі жоқ немесе бос");
        }
        return v.asText();
    }
}
