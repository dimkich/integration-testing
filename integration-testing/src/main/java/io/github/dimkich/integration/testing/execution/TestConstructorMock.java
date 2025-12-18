package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares constructor mocking configuration for integration tests.
 * <p>
 * This annotation is processed by {@link IntegrationTesting} to replace or spy
 * on constructor calls of the specified class during a test run.
 * </p>
 * <p>
 * It can be declared multiple times on the same test class via
 * {@link TestConstructorMock.List}.
 * </p>
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(TestConstructorMock.List.class)
public @interface TestConstructorMock {
    /**
     * Optional logical name of this mock configuration.
     * <p>
     * May be used for identification when several constructor mocks are
     * declared on the same test class.
     * </p>
     */
    String name() default "";

    /**
     * Target class whose constructors should be mocked.
     * <p>
     * Either this attribute or {@link #mockClassName()} must be specified.
     * When both are specified, this one typically has precedence.
     * </p>
     */
    Class<?> mockClass() default Null.class;

    /**
     * Fully qualified name of the target class whose constructors
     * should be mocked.
     * <p>
     * This is useful when the class is not available at compile time.
     * </p>
     */
    String mockClassName() default "";

    /**
     * Names of constructors (signatures) or factory methods to be mocked.
     * <p>
     * An empty array usually means that all constructors of the target class
     * are subject to mocking.
     * </p>
     */
    String[] methods() default {};

    /**
     * Whether the created instances should be spies instead of full mocks.
     * <p>
     * When {@code true}, the original constructor executes, but the created
     * instance is wrapped, allowing selective stubbing and verification.
     * </p>
     */
    boolean spy() default false;

    /**
     * Whether arguments passed to the constructor and the created instance
     * (result) should be cloned before being exposed to the test.
     * <p>
     * This is helpful to avoid accidental modification of data that is also
     * used by the system under test.
     * </p>
     */
    boolean cloneArgsAndResult() default false;

    /**
     * Container annotation that allows multiple {@link TestConstructorMock}
     * declarations on the same test class.
     */
    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @Documented
    @interface List {
        /**
         * Collection of {@link TestConstructorMock} declarations.
         */
        TestConstructorMock[] value();
    }
}
