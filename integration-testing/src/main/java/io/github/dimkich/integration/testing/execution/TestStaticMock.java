package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures a static mock to be used in an integration test.
 * <p>
 * This annotation is applied on a test class to define how a particular static
 * type should be mocked (or spied) during test execution.
 * It can be declared multiple times on the same class via {@link TestStaticMock.List}.
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(TestStaticMock.List.class)
public @interface TestStaticMock {
    /**
     * Optional logical name of this static mock configuration.
     * <p>
     * Can be used to distinguish multiple mocks of the same type.
     */
    String name() default "";

    /**
     * Type whose static methods will be mocked.
     * <p>
     * Either this attribute or {@link #mockClassName()} must be specified.
     */
    Class<?> mockClass() default Null.class;

    /**
     * Fully qualified name of the type whose static methods will be mocked.
     * <p>
     * This is useful when the class is not directly accessible on the test classpath.
     * Either this attribute or {@link #mockClass()} must be specified.
     */
    String mockClassName() default "";

    /**
     * Names of static methods of the target type that should be mocked or spied.
     * <p>
     * If empty, the configuration may apply to all static methods depending on the implementation.
     */
    String[] methods() default {};

    /**
     * If {@code true}, the specified static methods will be spied instead of fully mocked.
     * <p>
     * In spy mode, real method implementations are used unless explicitly stubbed.
     */
    boolean spy() default false;

    /**
     * If {@code true}, arguments and return values of mocked calls are deep-cloned.
     * <p>
     * This helps to avoid side effects caused by shared mutable state between the test
     * and the mocked static method invocations.
     */
    boolean cloneArgsAndResult() default false;

    /**
     * Container annotation that allows repeating {@link TestStaticMock} on the same test class.
     */
    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @Documented
    @interface List {
        /**
         * Collection of {@link TestStaticMock} declarations.
         */
        TestStaticMock[] value();
    }
}
