package io.github.dimkich.integration.testing.wait.completion;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Internal annotation used to bind a textual pointcut selector to Byte Buddy advice.
 * <p>
 * Instances of this annotation are not written in user code. Instead, it is
 * referenced from instrumentation configuration (for example in
 * {@link io.github.dimkich.integration.testing.wait.completion.future.like.FutureLikeWaitCompletion})
 * where {@code Advice.withCustomMapping().bind(Pointcut.class, selector)} supplies
 * the {@link #value()} passed to advice parameters annotated with {@code @Pointcut}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Pointcut {

    /**
     * Textual representation of the pointcut selector that matched the instrumented
     * method or constructor.
     *
     * @return selector string associated with the current advice invocation
     */
    String value() default "";
}
