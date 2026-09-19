package io.github.dimkich.integration.testing.execution.hook;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Executes a custom {@link MethodAction} right after entering a method or constructor
 * matching the {@link #pointcut()}.
 *
 * <p>Available variables in {@link #pointcut()}: {@code t} (TypeDescriptionWrapper),
 * {@code m} (MethodDescriptionWrapper).</p>
 */
@Inherited
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(OnMethodEnter.List.class)
@IntegrationTesting
public @interface OnMethodEnter {
    /**
     * Byte Buddy pointcut expression selecting the methods to intercept.
     *
     * @return the pointcut expression
     */
    String pointcut();

    /**
     * SpEL condition evaluated before the action runs; the action is skipped
     * when the expression evaluates to {@code false}.
     *
     * @return the condition expression, {@code "true"} by default
     */
    String when() default "true";

    /**
     * Action executed after entering a matching method or constructor.
     *
     * @return the action class; must have a public no-args constructor
     */
    Class<? extends MethodAction> action();

    /**
     * Container annotation allowing {@link OnMethodEnter} to be repeated
     * on the same type.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        /**
         * @return the repeated annotations
         */
        OnMethodEnter[] value();
    }
}
