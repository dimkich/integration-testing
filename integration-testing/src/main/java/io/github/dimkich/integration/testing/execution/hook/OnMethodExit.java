package io.github.dimkich.integration.testing.execution.hook;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Executes a custom {@link MethodAction} when a method or constructor matching the
 * {@link #pointcut()} exits, both normally and with an exception.
 *
 * <p>Available variables in {@link #pointcut()}: {@code t} (TypeDescriptionWrapper),
 * {@code m} (MethodDescriptionWrapper).</p>
 *
 * <p><b>Note:</b> the exit hook is applied to methods only. ByteBuddy cannot catch an
 * exception during a constructor call, therefore constructors are excluded from the
 * pointcut — use {@link OnMethodEnter} to hook constructors.</p>
 */
@Inherited
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(OnMethodExit.List.class)
@IntegrationTesting
public @interface OnMethodExit {
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
     * Action executed when a matching method exits, both normally and with an exception.
     *
     * @return the action class; must have a public no-args constructor
     */
    Class<? extends MethodAction> action();

    /**
     * Container annotation allowing {@link OnMethodExit} to be repeated
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
        OnMethodExit[] value();
    }
}
