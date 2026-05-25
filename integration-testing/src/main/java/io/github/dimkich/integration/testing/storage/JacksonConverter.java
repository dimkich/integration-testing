package io.github.dimkich.integration.testing.storage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.NameTransformer;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts Java POJOs to {@link Map} structures using Jackson's serialization rules.
 * <p>
 * Uses the {@link ObjectMapper} from {@link CompositeTestMapper} to introspect bean properties,
 * respecting {@link JsonInclude} annotations for property inclusion. Supports unwrapping
 * via {@code @JsonUnwrapped} and handles {@code @JsonAnyGetter} for dynamic properties.
 * </p>
 */
@RequiredArgsConstructor
public class JacksonConverter {
    private final CompositeTestMapper compositeTestMapper;

    /**
     * Converts a POJO to a map of property names to values.
     * <p>
     * Introspects the object's bean properties and includes only those that would be
     * serialized by Jackson, according to {@link JsonInclude} settings. Properties with
     * {@code @JsonUnwrapped} are flattened into the result; properties from
     * {@code @JsonAnyGetter} are merged into the result map.
     * </p>
     *
     * @param obj the object to convert; may be {@code null}
     * @return a map of property names to values, never {@code null}; empty if {@code obj} is null
     */
    @SneakyThrows
    public Map<String, Object> convertPojo(Object obj) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (obj == null) {
            return result;
        }

        ObjectMapper mapper = (ObjectMapper) compositeTestMapper.unwrap();
        SerializationConfig config = mapper.getSerializationConfig();
        JavaType type = mapper.getTypeFactory().constructType(obj.getClass());
        BeanDescription desc = config.introspect(type);
        AnnotationIntrospector introspector = config.getAnnotationIntrospector();
        JsonInclude.Value defaultInclusion = config.getDefaultPropertyInclusion();

        for (BeanPropertyDefinition prop : desc.findProperties()) {
            if (!prop.couldSerialize()) {
                continue;
            }
            AnnotatedMember accessor = prop.getAccessor();
            if (accessor == null) {
                continue;
            }
            Object value = accessor.getValue(obj);
            JsonInclude.Value propInclusion = prop.findInclusion().withOverrides(defaultInclusion);
            if (!shouldInclude(value, propInclusion.getValueInclusion())) {
                continue;
            }
            NameTransformer unwrapper = introspector.findUnwrappingNameTransformer(accessor);
            if (unwrapper != null && value != null) {
                Map<String, Object> unwrappedMap = convertPojo(value);
                unwrappedMap.forEach((k, v) -> result.put(unwrapper.transform(k), v));
            } else {
                result.put(prop.getName(), value);
            }
        }

        AnnotatedMember anyGetter = desc.findAnyGetter();
        if (anyGetter != null) {
            Object anyValue = anyGetter.getValue(obj);
            if (anyValue instanceof Map<?, ?> anyMap) {
                anyMap.forEach((k, v) -> result.put(String.valueOf(k), v));
            }
        }

        return result;
    }

    /**
     * Determines whether a property value should be included based on Jackson's
     * {@link JsonInclude.Include} rules.
     *
     * @param value   the property value
     * @param include the inclusion strategy (ALWAYS, NON_NULL, NON_ABSENT, NON_EMPTY)
     * @return {@code true} if the value should be included in the output
     */
    private boolean shouldInclude(Object value, JsonInclude.Include include) {
        if (include == JsonInclude.Include.ALWAYS) {
            return true;
        }
        if (value == null) {
            return include != JsonInclude.Include.NON_NULL
                    && include != JsonInclude.Include.NON_ABSENT
                    && include != JsonInclude.Include.NON_EMPTY;
        }

        if (include == JsonInclude.Include.NON_EMPTY) {
            return !isEmpty(value);
        }
        if (include == JsonInclude.Include.NON_ABSENT) {
            if (value instanceof Optional<?> opt) {
                return opt.isPresent();
            }
        }
        return true;
    }

    /**
     * Checks if the given value is considered empty for {@link JsonInclude.Include#NON_EMPTY}.
     * <p>
     * Empty means: empty map, empty collection, empty string, empty array, or empty Optional.
     * </p>
     *
     * @param value the value to check
     * @return {@code true} if the value is empty
     */
    private boolean isEmpty(Object value) {
        if (value instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        if (value instanceof Collection<?> c) {
            return c.isEmpty();
        }
        if (value instanceof String s) {
            return s.isEmpty();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value) == 0;
        }
        if (value instanceof Optional<?> opt) {
            return opt.isEmpty();
        }
        return false;
    }
}
