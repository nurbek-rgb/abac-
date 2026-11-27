package kz.abac;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Сұраныс атрибуттары: role, time, device, location, action (және кез келген басқа). */
public final class AttributeContext {
    private final Map<String, String> attributes;

    private AttributeContext(Map<String, String> attributes) {
        this.attributes = Map.copyOf(attributes);
    }

    /** Атрибут мәнін қайтарады; жоқ немесе бос болса, Optional.empty(). */
    public Optional<String> get(String name) {
        String value = attributes.get(name);
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value.trim());
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Immutable snapshot for audit records and concurrent evaluation. */
    public Map<String, String> attributes() { return attributes; }

    public static final class Builder {
        private final Map<String, String> map = new HashMap<>();

        public Builder role(String v) { return attribute("role", v); }
        public Builder device(String v) { return attribute("device", v); }
        public Builder location(String v) { return attribute("location", v); }
        public Builder action(String v) { return attribute("action", v); }

        /** Уақыт HH:mm мәтіні ретінде, мысалы "09:30". */
        public Builder time(String v) { return attribute("time", v); }

        public Builder time(LocalTime t) {
            return attribute("time", t == null ? null : t.format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        public Builder attribute(String name, String value) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Attribute name is required");
            if (value != null) {
                map.put(name, value);
            } else {
                map.remove(name);
            }
            return this;
        }

        public AttributeContext build() {
            return new AttributeContext(map);
        }
    }

    @Override
    public String toString() {
        return attributes.toString();
    }
}

