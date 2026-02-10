package io.github.dimkich.integration.testing.expression;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to bind a unique pointcut identifier to a ByteBuddy Advice method parameter.
 *
 * <p>In a multi-pointcut environment, this annotation allows the instrumentation logic
 * to distinguish which specific rule triggered the advice. It is typically used with
 * {@link net.bytebuddy.asm.Advice.OffsetMapping} or custom argument binding during
 * the transformation process.</p>
 *
 * @see PointcutMatch#apply(net.bytebuddy.agent.builder.AgentBuilder, Class)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PointcutId {
    /**
     * The unique integer identifier of the pointcut.
     *
     * @return the assigned pointcut ID.
     */
    int value() default 0;
}
