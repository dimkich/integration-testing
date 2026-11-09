package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures a Spring bean stub to be used in an integration test.
 * <p>
 * This annotation is applied on a test class to define how a particular Spring bean
 * should be stubbed (or spied) during test execution. The bean can be identified by
 * its name using {@link #name()}, or by its type using {@link #stubClass()} or
 * {@link #stubClassName()}.
 * It can be declared multiple times on the same class via {@link StubBean.List}.
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(StubBean.List.class)
public @interface StubBean {
    /**
     * Name of the Spring bean to stub.
     * <p>
     * If specified, the stub configuration applies to the bean with this name.
     * If empty, the bean will be matched by its type using {@link #stubClass()}
     * or {@link #stubClassName()}.
     */
    String name() default "";

    /**
     * Type of the bean whose methods will be stubbed.
     * <p>
     * If {@link #name()} is not specified, beans of this type will be stubbed.
     * Either this attribute or {@link #stubClassName()} must be specified when
     * not using {@link #name()}.
     */
    Class<?> stubClass() default Null.class;

    /**
     * Fully qualified name of the bean type whose methods will be stubbed.
     * <p>
     * This is useful when the class is not directly accessible on the test classpath.
     * If {@link #name()} is not specified, beans of this type will be stubbed.
     * Either this attribute or {@link #stubClass()} must be specified when
     * not using {@link #name()}.
     */
    String stubClassName() default "";

    /**
     * Names of methods of the target bean that should be stubbed or spied.
     * <p>
     * If empty, the configuration may apply to all methods depending on the implementation.
     */
    String[] methods() default {};

    /**
     * If {@code true}, the specified bean methods will be spied instead of fully stubbed.
     * <p>
     * In spy mode, real method implementations are used unless explicitly stubbed.
     */
    boolean spy() default false;

    /**
     * If {@code true}, arguments and return values of stubbed calls are deep-cloned.
     * <p>
     * This helps to avoid side effects caused by shared mutable state between the test
     * and the stubbed bean method invocations.
     */
    boolean cloneArgsAndResult() default false;

    /**
     * Container annotation that allows repeating {@link StubBean} on the same test class.
     */
    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @Documented
    @interface List {
        /**
         * Collection of {@link StubBean} declarations.
         */
        StubBean[] value();
    }
}
