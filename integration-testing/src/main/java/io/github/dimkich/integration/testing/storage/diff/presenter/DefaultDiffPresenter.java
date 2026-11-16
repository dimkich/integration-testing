package io.github.dimkich.integration.testing.storage.diff.presenter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.util.AnnotationUtil;
import io.github.dimkich.integration.testing.config.SimpleTypeDetector;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import io.github.dimkich.integration.testing.storage.diff.format.ValueFormat;
import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.util.FastPath;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.lang.NonNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Describes a scalar, an array or an object with properties.
 * <p>
 * A scalar is written as text, an array as unnamed elements, an object as named fields. The
 * fields of an object come from its Jackson projection: properties serialized as XML attributes
 * become metadata attributes, the remaining properties become body fields. The projection keeps
 * the serialization order of the source and respects {@link JsonInclude},
 * {@code @JsonUnwrapped} and {@code @JsonAnyGetter}.
 * <p>
 * The Jackson description of a class is cached per mapper: Jackson does not cache introspection
 * itself, so without the cache every value of the type would run the full annotation scan. The
 * mapper is part of the key because XML and JSON read the XML annotations differently.
 */
@RequiredArgsConstructor
public class DefaultDiffPresenter implements DiffPresenter<Object> {
    private final SimpleTypeDetector simpleTypeDetector;
    private final ValueFormat valueFormat;
    private final CompositeTestMapper testMapper;

    private final ClassValue<Map<ObjectMapper, Projection>> projections = new ClassValue<>() {
        @Override
        protected Map<ObjectMapper, Projection> computeValue(@NonNull Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    @Override
    public Object present(Object value, DiffAttributes.Builder attributes) {
        Class<?> type = value.getClass();
        if (FastPath.isScalarOrArray(type, simpleTypeDetector)) {
            return value;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, String> attributeValues = project(value, null, body, null, testMapper.unwrap());
        attributes.addAttributes(attributeValues);
        return body;
    }

    /**
     * Adds the serialized properties of an object to the body and the attributes.
     *
     * @param value       object to project
     * @param transformer renaming of the enclosing {@code @JsonUnwrapped} properties, or
     *                    {@code null} at the top level
     * @param body        body fields by serialized name
     * @param attributes  metadata attributes of the element by name, {@code null} until the first
     *                    attribute is found
     * @param mapper      mapper of the current test format
     * @return metadata attributes, or {@code null} when the object has none
     */
    @SneakyThrows
    private Map<String, String> project(Object value, NameTransformer transformer,
                                        Map<String, Object> body, Map<String, String> attributes,
                                        ObjectMapper mapper) {
        Projection projection = projection(value.getClass(), mapper);
        for (Property property : projection.properties()) {
            Object propertyValue = property.accessor().getValue(value);
            if (!shouldInclude(propertyValue, property.inclusion())) {
                continue;
            }
            if (property.unwrapper() != null && propertyValue != null) {
                attributes = project(propertyValue, chain(transformer, property.unwrapper()),
                        body, attributes, mapper);
                continue;
            }
            String name = transform(transformer, property.name());
            if (property.attribute()) {
                if (propertyValue != null) {
                    if (attributes == null) {
                        attributes = new LinkedHashMap<>();
                    }
                    attributes.put(name, valueFormat.text(propertyValue));
                }
            } else {
                body.put(name, propertyValue);
            }
        }

        AnnotatedMember anyGetter = projection.anyGetter();
        if (anyGetter != null) {
            Object anyValue = anyGetter.getValue(value);
            if (anyValue instanceof Map<?, ?> anyMap) {
                anyMap.forEach((key, propertyValue) ->
                        body.put(transform(transformer, String.valueOf(key)), propertyValue));
            }
        }
        return attributes;
    }

    /**
     * Returns the cached Jackson projection of a class for the current mapper. Property descriptors
     * are built once: the per-value work is reduced to reading the property values.
     */
    private Projection projection(Class<?> type, ObjectMapper mapper) {
        Map<ObjectMapper, Projection> byMapper = projections.get(type);
        Projection projection = byMapper.get(mapper);
        if (projection != null) {
            return projection;
        }
        Projection computed = createProjection(type, mapper);
        Projection previous = byMapper.putIfAbsent(mapper, computed);
        return previous == null ? computed : previous;
    }

    /**
     * Builds the immutable description of the properties of a class: the accessor, the serialized
     * name, the inclusion rule, the unwrapping transformer and the XML attribute flag.
     */
    private Projection createProjection(Class<?> type, ObjectMapper mapper) {
        SerializationConfig config = mapper.getSerializationConfig();
        BeanDescription description = config.introspect(mapper.getTypeFactory().constructType(type));
        AnnotationIntrospector introspector = config.getAnnotationIntrospector();
        JsonInclude.Value defaultInclusion = config.getDefaultPropertyInclusion();
        List<Property> properties = new ArrayList<>();
        for (BeanPropertyDefinition property : description.findProperties()) {
            AnnotatedMember accessor = property.getAccessor();
            if (!property.couldSerialize() || accessor == null) {
                continue;
            }
            JsonInclude.Include inclusion = defaultInclusion
                    .withOverrides(property.findInclusion()).getValueInclusion();
            NameTransformer unwrapper = introspector.findUnwrappingNameTransformer(accessor);
            boolean attribute = Boolean.TRUE.equals(
                    AnnotationUtil.findIsAttributeAnnotation(config, introspector, accessor));
            properties.add(new Property(accessor, property.getName(), inclusion, unwrapper, attribute));
        }
        return new Projection(properties, description.findAnyGetter());
    }

    private record Projection(List<Property> properties, AnnotatedMember anyGetter) {
    }

    private record Property(AnnotatedMember accessor, String name, JsonInclude.Include inclusion,
                            NameTransformer unwrapper, boolean attribute) {
    }

    private static NameTransformer chain(NameTransformer outer, NameTransformer inner) {
        return outer == null ? inner : NameTransformer.chainedTransformer(inner, outer);
    }

    private static String transform(NameTransformer transformer, String name) {
        return transformer == null ? name : transformer.transform(name);
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
        if (FastPath.isArray(value.getClass())) {
            return Array.getLength(value) == 0;
        }
        if (value instanceof Optional<?> opt) {
            return opt.isEmpty();
        }
        return false;
    }
}
