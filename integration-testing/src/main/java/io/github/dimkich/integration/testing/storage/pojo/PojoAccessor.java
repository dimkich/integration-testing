package io.github.dimkich.integration.testing.storage.pojo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides reflective access to properties of a POJO (Plain Old Java Object).
 * <p>
 * Wraps a target instance and a map of property names to {@link PojoProperty} descriptors,
 * allowing get/set operations by property name and conversion to a {@link Map} representation.
 */
@RequiredArgsConstructor
public class PojoAccessor {
    @Getter
    private final Object target;
    private final Map<String, PojoProperty> properties;

    /**
     * Returns the value of the named property.
     *
     * @param name the property name
     * @return the property value, or {@code null} if the property is not found
     */
    public Object getPropertyValue(String name) {
        PojoProperty prop = properties.get(name);
        return (prop != null) ? prop.getValue(target) : null;
    }

    /**
     * Sets the value of the named property.
     *
     * @param name  the property name
     * @param value the value to set; {@code null} sets primitive fields to their default value
     */
    public void setPropertyValue(String name, Object value) {
        PojoProperty prop = properties.get(name);
        if (prop != null) {
            prop.setValue(target, value);
        }
    }

    /**
     * Returns the declared type of the named property.
     *
     * @param name the property name
     * @return the property type, or {@code null} if the property is not found
     */
    public Class<?> getPropertyType(String name) {
        PojoProperty prop = properties.get(name);
        return (prop != null) ? prop.getField().getType() : null;
    }

    /**
     * Converts the target object to a map of property names to values.
     * Property order is preserved as defined in the underlying properties map.
     *
     * @return a new map containing all property names and their current values
     */
    public Map<String, Object> asMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        properties.forEach((name, prop) -> result.put(name, prop.getValue(target)));
        return result;
    }
}
