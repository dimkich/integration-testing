package io.github.dimkich.integration.testing.collections;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Makes collection iteration order deterministic inside the selected class/package prefixes.
 * <p>
 * The annotation activates a Byte Buddy instrumentation plugin that replaces JDK collection
 * factories and hash-based collection constructors with ordered analogues:
 * <ul>
 *     <li>{@link java.util.Map#of} and {@link java.util.Map#ofEntries} &rarr; unmodifiable
 *         {@link java.util.LinkedHashMap} (insertion order);</li>
 *     <li>{@link java.util.Map#copyOf} and {@link java.util.Set#copyOf} &rarr; unmodifiable
 *         ordered copies;</li>
 *     <li>{@link java.util.Set#of} &rarr; unmodifiable {@link java.util.LinkedHashSet};</li>
 *     <li>{@link java.util.stream.Collectors#toSet()},
 *         {@link java.util.stream.Collectors#toMap},
 *         {@link java.util.stream.Collectors#groupingBy} and their unmodifiable variants
 *         &rarr; ordered collectors;</li>
 *     <li>{@code new HashMap(...)} and {@code new HashSet(...)} &rarr;
 *         {@code new LinkedHashMap(...)} and {@code new LinkedHashSet(...)}.</li>
 * </ul>
 * <p>
 * Only the iteration order changes. The JDK contracts are preserved: mutability, null
 * handling and duplicate checks remain exactly the same as in the replaced methods.
 * </p>
 * <p>
 * Instrumentation is limited to classes whose fully qualified names start with one of the
 * prefixes in {@link #value()}. The test class itself and any class loaded before the agent
 * installation are retransformed automatically; if another tool reloads classes later,
 * {@link io.github.dimkich.integration.testing.instrumentation.RepeatInstrumentation} can be
 * used to reapply the transformation.
 * </p>
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @DeterministicCollections("com.example.myapp")
 * public class MyIntegrationTest {
 *     // ...
 * }
 * }</pre>
 * <p><b>Known limitations:</b></p>
 * <ul>
 *     <li>{@code Map::ofEntries} and similar method references are not intercepted because
 *         they are compiled to {@code invokedynamic};</li>
 *     <li>anonymous subclasses like {@code new HashMap<>() {{ ... }}} remain
 *         {@link java.util.HashMap} subclasses.</li>
 * </ul>
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@Documented
@IntegrationTesting
public @interface DeterministicCollections {

    /**
     * Class and/or package name prefixes whose bytecode is instrumented.
     * <p>
     * Each value is treated as a "starts with" pattern on the fully qualified class name.
     * For example, {@code "com.example"} instruments the whole {@code com.example} package
     * tree, while {@code "com.example.service.OrderService"} covers that class and its inner
     * classes.
     * </p>
     */
    String[] value();

    /**
     * Feature groups that should be disabled.
     * <p>
     * All groups are enabled by default. Disabling {@link Feature#MAP_OF},
     * {@link Feature#SET_OF}, {@link Feature#COPY_OF} or {@link Feature#COLLECTORS}
     * deliberately reintroduces non-deterministic iteration order, so this switch is intended
     * for diagnostics and for the risky {@link Feature#HASH_COLLECTIONS} replacement only.
     * </p>
     */
    Feature[] disabled() default {};

    /**
     * Groups of replacements that can be disabled independently.
     */
    enum Feature {
        /**
         * Replaces {@link java.util.Map#of} and {@link java.util.Map#ofEntries}.
         */
        MAP_OF,

        /**
         * Replaces {@link java.util.Map#copyOf} and {@link java.util.Set#copyOf}.
         */
        COPY_OF,

        /**
         * Replaces {@link java.util.Set#of}.
         */
        SET_OF,

        /**
         * Replaces ordered-sensitive collectors: {@code toSet}, {@code toUnmodifiableSet},
         * {@code toMap}, {@code toUnmodifiableMap} and {@code groupingBy}.
         */
        COLLECTORS,

        /**
         * Replaces {@code new HashMap(...)} and {@code new HashSet(...)} with
         * {@code LinkedHashMap} and {@code LinkedHashSet}.
         */
        HASH_COLLECTIONS
    }
}
