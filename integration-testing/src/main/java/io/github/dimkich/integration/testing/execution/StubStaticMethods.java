package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures static method stubs to be used in an integration test.
 * <p>
 * This annotation is applied on a test class to define how static methods of a
 * particular type should be stubbed (or spied) during test execution.
 * It can be declared multiple times on the same class via {@link StubStaticMethods.List}.
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(StubStaticMethods.List.class)
public @interface StubStaticMethods {
    /**
     * Optional logical name of this static stub configuration.
     * <p>
     * Can be used to distinguish multiple stubs of the same type.
     */
    String name() default "";

    /**
     * Type whose static methods will be stubbed.
     * <p>
     * Either this attribute or {@link #stubClassName()} must be specified.
     */
    Class<?> stubClass() default Null.class;

    /**
     * Fully qualified name of the type whose static methods will be stubbed.
     * <p>
     * This is useful when the class is not directly accessible on the test classpath.
     * Either this attribute or {@link #stubClass()} must be specified.
     */
    String stubClassName() default "";

    /**
     * Names of static methods of the target type that should be stubbed or spied.
     * <p>
     * If empty, the configuration may apply to all static methods depending on the implementation.
     */
    String[] methods() default {};

    /**
     * If {@code true}, the specified static methods will be spied instead of fully stubbed.
     * <p>
     * In spy mode, real method implementations are used unless explicitly stubbed.
     */
    boolean spy() default false;

    /**
     * If {@code true}, arguments and return values of stubbed calls are deep-cloned.
     * <p>
     * This helps to avoid side effects caused by shared mutable state between the test
     * and the stubbed static method invocations.
     */
    boolean cloneArgsAndResult() default false;

    /**
     * Container annotation that allows repeating {@link StubStaticMethods} on the same test class.
     */
    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @Documented
    @interface List {
        /**
         * Collection of {@link StubStaticMethods} declarations.
         */
        StubStaticMethods[] value();
    }
}
