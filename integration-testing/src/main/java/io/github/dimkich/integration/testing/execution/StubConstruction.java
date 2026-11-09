package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares construction stubbing configuration for integration tests.
 * <p>
 * This annotation is processed by {@link IntegrationTesting} to replace or spy
 * on constructor calls of the specified class during a test run.
 * </p>
 * <p>
 * It can be declared multiple times on the same test class via
 * {@link StubConstruction.List}.
 * </p>
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(StubConstruction.List.class)
public @interface StubConstruction {
    /**
     * Optional logical name of this stub configuration.
     * <p>
     * May be used for identification when several construction stubs are
     * declared on the same test class.
     * </p>
     */
    String name() default "";

    /**
     * Target class whose constructors should be stubbed.
     * <p>
     * Either this attribute or {@link #stubClassName()} must be specified.
     * When both are specified, this one typically has precedence.
     * </p>
     */
    Class<?> stubClass() default Null.class;

    /**
     * Fully qualified name of the target class whose constructors
     * should be stubbed.
     * <p>
     * This is useful when the class is not available at compile time.
     * </p>
     */
    String stubClassName() default "";

    /**
     * Names of constructors (signatures) or factory methods to be stubbed.
     * <p>
     * An empty array usually means that all constructors of the target class
     * are subject to stubbing.
     * </p>
     */
    String[] methods() default {};

    /**
     * Whether the created instances should be spies instead of full stubs.
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
     * Container annotation that allows multiple {@link StubConstruction}
     * declarations on the same test class.
     */
    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @Documented
    @interface List {
        /**
         * Collection of {@link StubConstruction} declarations.
         */
        StubConstruction[] value();
    }
}
