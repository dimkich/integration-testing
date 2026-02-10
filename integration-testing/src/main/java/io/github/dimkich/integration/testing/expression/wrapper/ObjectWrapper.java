package io.github.dimkich.integration.testing.expression.wrapper;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A sophisticated wrapper for runtime objects, providing a fluent and safe API for
 * reflection-based operations within expressions.
 *
 * <p>In the expression DSL, this is represented by the {@code o} variable. It simplifies
 * common tasks such as type checking, field access, and method invocation by handling
 * primitive boxing/unboxing and type widening automatically.</p>
 *
 * <p><b>Example expressions:</b>
 * <ul>
 *   <li>{@code o.asString().startsWith('test')} — typical string operation.</li>
 *   <li>{@code o.field('status').asInt() == 1} — accessing private fields.</li>
 *   <li>{@code o.call('substring', 1, 3).asString()} — reflective method call.</li>
 *   <li>{@code o.isInstance(java.util.List.class)} — dynamic type checking.</li>
 * </ul></p>
 */
@RequiredArgsConstructor
public class ObjectWrapper {

    /**
     * A constant representing a {@code null} target object.
     */
    public static final ObjectWrapper NULL = new ObjectWrapper(null);

    /**
     * The actual runtime object being wrapped.
     */
    private final Object target;

    /**
     * @return {@code true} if the wrapped object is {@code null}.
     */
    public boolean isNull() {
        return target == null;
    }

    /**
     * Checks if the wrapped object is an instance of the specified class.
     * <p>Automatically handles primitive types by comparing against their unwrapped forms.</p>
     *
     * @param type the class to check against.
     * @return {@code true} if the object can be cast to the specified type.
     * @example {@code o.isInstance(int.class)} returns true for an {@code Integer} object.
     */
    public boolean isInstance(Class<?> type) {
        if (target == null) {
            return false;
        }
        if (type.isPrimitive()) {
            return TypeUtils.unwrap(target.getClass()) == type;
        }
        return type.isInstance(target);
    }

    /**
     * Checks if the wrapped object is exactly of the specified class.
     *
     * @param type the class to compare with.
     * @return {@code true} if the object's class matches the specified type exactly.
     * @example {@code o.isSameClass(java.util.List.class)} returns false for an {@code ArrayList}.
     */
    public boolean isSameClass(Class<?> type) {
        if (target == null) {
            return false;
        }
        Class<?> targetClass = target.getClass();
        if (type.isPrimitive()) {
            return TypeUtils.unwrap(targetClass) == type;
        }
        return targetClass == type;
    }

    /**
     * @return the raw underlying object.
     */
    public Object get() {
        return target;
    }

    // --- Type Casting Methods ---

    /**
     * @return the object cast to {@code boolean}.
     */
    public boolean asBoolean() {
        return (boolean) target;
    }

    /**
     * @return the object cast to {@code char}.
     */
    public char asChar() {
        return (char) target;
    }

    /**
     * @return the object cast to {@code byte}.
     */
    public byte asByte() {
        return (byte) target;
    }

    /**
     * @return the object cast to {@code short}.
     */
    public short asShort() {
        return (short) target;
    }

    /**
     * @return the object cast to {@code int}.
     */
    public int asInt() {
        return (int) target;
    }

    /**
     * @return the object cast to {@code long}.
     */
    public long asLong() {
        return (long) target;
    }

    /**
     * @return the object cast to {@code float}.
     */
    public float asFloat() {
        return (float) target;
    }

    /**
     * @return the object cast to {@code double}.
     */
    public double asDouble() {
        return (double) target;
    }

    /**
     * @return the object cast to {@link Iterable}.
     */
    public Iterable<?> asIterable() {
        return (Iterable<?>) target;
    }

    /**
     * @return the object cast to {@link Collection}.
     */
    public Collection<?> asCollection() {
        return (Collection<?>) target;
    }

    /**
     * @return the object cast to {@link List}.
     */
    public List<?> asList() {
        return (List<?>) target;
    }

    /**
     * @return the object cast to {@link Map}.
     */
    public Map<?, ?> asMap() {
        return (Map<?, ?>) target;
    }

    /**
     * @return the object cast to {@link String}.
     */
    public String asString() {
        return (String) target;
    }

    /**
     * Accesses a field on the target object by name via reflection.
     * <p>Searches recursively through the class hierarchy and makes the field accessible.</p>
     *
     * @param name the name of the field.
     * @return an {@code ObjectWrapper} containing the field value, or {@link #NULL} if target is null.
     * @throws Exception if the field cannot be found or accessed.
     * @example {@code o.field('myPrivateField')}
     */
    public ObjectWrapper field(String name) throws Exception {
        if (target == null) {
            return ObjectWrapper.NULL;
        }
        Field f = TypeUtils.getField(target.getClass(), name);
        return new ObjectWrapper(f.get(target));
    }

    /**
     * Invokes a method on the target object via reflection.
     * <p>Uses advanced parameter matching (widening and boxing) to find the most
     * suitable method signature for the provided arguments.</p>
     *
     * @param name the method name.
     * @param args the arguments for the call.
     * @return an {@code ObjectWrapper} containing the return value, or {@link #NULL} if target is null.
     * @throws Exception if the method is missing, ambiguous, or invocation fails.
     * @example {@code o.call('substring', 0, 4)}
     */
    public ObjectWrapper call(String name, Object... args) throws Exception {
        if (target == null) {
            return ObjectWrapper.NULL;
        }
        Class<?>[] types = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
        Method m = TypeUtils.getMethod(target.getClass(), name, types);
        return new ObjectWrapper(m.invoke(target, args));
    }
}
