package io.github.dimkich.integration.testing.expression.wrapper;

import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import lombok.SneakyThrows;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal utility engine for type resolution, reflection, and assignment compatibility.
 *
 * <p>This class bridges the gap between static ByteBuddy type descriptions and
 * dynamic runtime reflection. It implements Java Language Specification (JLS)
 * compatible <b>primitive widening</b> and recursive member lookups, allowing
 * expressions to invoke methods and access fields even across complex class hierarchies.</p>
 *
 * <p>Key features:
 * <ul>
 *   <li><b>Caching:</b> High-performance caching for resolved types, fields, and methods.</li>
 *   <li><b>Recursive Lookup:</b> Searches through superclasses for private or inherited members.</li>
 *   <li><b>Smart Matching:</b> Resolves methods by accounting for boxing and widening (e.g., matching {@code int} to {@code long}).</li>
 * </ul></p>
 */
public class TypeUtils {
    /**
     * Global pool for resolving ByteBuddy type descriptions.
     */
    private static final TypePool TYPE_POOL = TypePool.Default.ofSystemLoader();

    /**
     * Cache for resolved {@link TypeDescription} instances.
     */
    private static final Map<String, TypeDescription> CACHE = new ConcurrentHashMap<>();

    /**
     * Cache for reflected fields to avoid repeated hierarchy traversal.
     */
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Cache for reflected methods based on their signatures and parameter types.
     */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();


    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = Map.of(
            Integer.class, int.class,
            Long.class, long.class,
            Boolean.class, boolean.class,
            Double.class, double.class,
            Float.class, float.class,
            Byte.class, byte.class,
            Short.class, short.class,
            Character.class, char.class
    );

    /**
     * Resolves a class name into a {@link TypeDescription}.
     * Supports primitives, standard classes, and array notation (e.g., {@code "int[]"}, {@code "java.lang.String[][]"}).
     *
     * @param name Fully Qualified Class Name or primitive keyword.
     * @return the resolved type description.
     */
    public static TypeDescription resolve(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            if (n.endsWith("[]")) {
                String componentName = n.substring(0, n.length() - 2);
                TypeDescription componentType = resolve(componentName); // Recursive call
                return TypeDescription.ArrayProjection.of(componentType);
            }
            return switch (n) {
                case "byte" -> TypeDescription.ForLoadedType.of(byte.class);
                case "int" -> TypeDescription.ForLoadedType.of(int.class);
                case "short" -> TypeDescription.ForLoadedType.of(short.class);
                case "long" -> TypeDescription.ForLoadedType.of(long.class);
                case "double" -> TypeDescription.ForLoadedType.of(double.class);
                case "float" -> TypeDescription.ForLoadedType.of(float.class);
                case "boolean" -> TypeDescription.ForLoadedType.of(boolean.class);
                case "char" -> TypeDescription.ForLoadedType.of(char.class);
                case "void" -> TypeDescription.ForLoadedType.of(void.class);
                default -> TYPE_POOL.describe(n).resolve();
            };
        });
    }

    /**
     * Unwraps a wrapper class to its corresponding primitive type.
     *
     * @param clazz the class to unwrap (e.g., {@code Integer.class}).
     * @return the primitive type (e.g., {@code int.class}) or the original class if no mapping exists.
     */
    public static Class<?> unwrap(Class<?> clazz) {
        return WRAPPER_TO_PRIMITIVE.getOrDefault(clazz, clazz);
    }

    /**
     * Finds and caches a field within the class hierarchy.
     *
     * @param clazz the starting class for the search.
     * @param name  the field name.
     * @return the field instance, made accessible for reflection.
     * @throws NoSuchFieldException if the field is not found in the hierarchy.
     */
    public static Field getField(Class<?> clazz, String name) {
        String key = clazz.getName() + "#" + name;
        return FIELD_CACHE.computeIfAbsent(key, k -> {
            Field f = findFieldRecursive(clazz, name);
            ByteBuddyUtils.makeAccessible(f);
            return f;
        });
    }

    /**
     * Finds and caches a method that best matches the given name and argument types.
     * <p>This method accounts for <b>assignability</b>, meaning it can find methods
     * that accept wider types (e.g., calling {@code doWork(long)} with an {@code int} argument).</p>
     *
     * @param clazz the starting class for the search.
     * @param name  the method name.
     * @param types the actual runtime types of the arguments.
     * @return the best matching method instance.
     * @throws NoSuchMethodException if no applicable method is found.
     */
    public static Method getMethod(Class<?> clazz, String name, Class<?>[] types) {
        String key = clazz.getName() + "#" + name + Arrays.toString(types);
        return METHOD_CACHE.computeIfAbsent(key, k -> {
            Method m = findMethodRecursive(clazz, name, types);
            ByteBuddyUtils.makeAccessible(m);
            return m;
        });
    }

    /**
     * Checks if a source type can be assigned to a target type according to Java rules.
     * <p>Handles identity conversion, boxing/unboxing, and widening primitive conversions.</p>
     *
     * @param target the required type (e.g., method parameter).
     * @param source the actual type (e.g., provided argument).
     * @return {@code true} if the assignment is valid.
     */
    public static boolean isAssignable(Class<?> target, Class<?> source) {
        if (target.isAssignableFrom(source)) {
            return true;
        }
        Class<?> targetUnwrapped = unwrap(target);
        Class<?> sourceUnwrapped = unwrap(source);
        if (targetUnwrapped.isPrimitive() && sourceUnwrapped.isPrimitive()) {
            return targetUnwrapped == sourceUnwrapped || canWiden(targetUnwrapped, sourceUnwrapped);
        }
        return false;
    }

    @SneakyThrows
    private static Field findFieldRecursive(Class<?> clazz, String name) {
        Class<?> curr = clazz;
        while (curr != null) {
            try {
                return curr.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                curr = curr.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    @SneakyThrows
    private static Method findMethodRecursive(Class<?> clazz, String name, Class<?>[] types) {
        Class<?> curr = clazz;
        while (curr != null) {
            try {
                return curr.getDeclaredMethod(name, types);
            } catch (NoSuchMethodException ignored) {
            }
            for (Method method : curr.getDeclaredMethods()) {
                if (method.getName().equals(name) && isAssignable(method.getParameterTypes(), types)) {
                    return method;
                }
            }
            curr = curr.getSuperclass();
        }
        throw new NoSuchMethodException(name + " with params " + Arrays.toString(types));
    }

    private static boolean isAssignable(Class<?>[] targetTypes, Class<?>[] sourceTypes) {
        if (targetTypes.length != sourceTypes.length) return false;
        for (int i = 0; i < targetTypes.length; i++) {
            if (!isAssignable(targetTypes[i], sourceTypes[i])) return false;
        }
        return true;
    }

    /**
     * Implements widening primitive conversion rules (JLS 5.1.2).
     *
     * @example {@code long} can accept {@code int}, {@code short}, {@code byte}, or {@code char}.
     */
    private static boolean canWiden(Class<?> target, Class<?> source) {
        if (target == long.class) {
            return source == int.class || source == short.class || source == byte.class || source == char.class;
        }
        if (target == int.class) {
            return source == short.class || source == byte.class || source == char.class;
        }
        if (target == double.class) {
            return source == float.class || source == int.class || source == long.class || source == char.class;
        }
        if (target == float.class) {
            return source == char.class || source == byte.class || source == short.class || source == int.class;
        }
        return false;
    }
}
