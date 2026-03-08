package io.github.dimkich.integration.testing.storage.pojo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Field;

/**
 * Represents a single POJO property backed by a {@link Field}.
 * <p>
 * Provides reflective get/set operations on the underlying field. When setting {@code null}
 * on a primitive-typed field, the field is set to its default value (e.g. 0, false)
 * instead of throwing, since primitives cannot hold {@code null}.
 *
 * @see PojoAccessor
 * @see PojoAccessorService
 */
@RequiredArgsConstructor
public class PojoProperty {
    /**
     * The reflected field for this property.
     */
    @Getter
    private final Field field;

    /**
     * Returns the value of this property on the given target instance.
     *
     * @param target the object instance to read from
     * @return the current field value
     */
    @SneakyThrows
    public Object getValue(Object target) {
        return field.get(target);
    }

    /**
     * Sets the value of this property on the given target instance.
     * <p>
     * If {@code value} is {@code null} and the field is a primitive type, the field
     * is set to its default value instead (e.g. 0 for numeric types, false for boolean).
     *
     * @param target the object instance to modify
     * @param value  the value to set; may be {@code null} (handled for primitives)
     */
    @SneakyThrows
    public void setValue(Object target, Object value) {
        if (value == null && field.getType().isPrimitive()) {
            setPrimitiveDefault(target);
        } else {
            field.set(target, value);
        }
    }

    /**
     * Sets the primitive field to its default value when {@code null} is passed.
     */
    private void setPrimitiveDefault(Object target) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (type == boolean.class) field.setBoolean(target, false);
        else if (type == char.class) field.setChar(target, ' ');
        else if (type == byte.class) field.setByte(target, (byte) 0);
        else if (type == short.class) field.setShort(target, (short) 0);
        else if (type == int.class) field.setInt(target, 0);
        else if (type == long.class) field.setLong(target, 0L);
        else if (type == float.class) field.setFloat(target, 0.0f);
        else if (type == double.class) field.setDouble(target, 0.0);
    }
}
