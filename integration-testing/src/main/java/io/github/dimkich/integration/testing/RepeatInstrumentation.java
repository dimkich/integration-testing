package io.github.dimkich.integration.testing;

import java.lang.annotation.*;

/**
 * Marks a test class for repeated re‑instrumentation of already loaded classes.
 * <p>
 * When a test class annotated with this annotation is executed via
 * {@link io.github.dimkich.integration.testing.execution.junit.JunitExtension},
 * the extension scans all loaded, modifiable classes and, for each name specified
 * in {@link #value()}, re-transforms every class whose fully qualified name starts
 * with that prefix.
 * <p>
 * This is useful when you need ByteBuddy transformations to be applied again
 * (for example after other instrumentation steps or context changes) without
 * restarting the JVM.
 *
 * @see io.github.dimkich.integration.testing.execution.junit.JunitExtension
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatInstrumentation {
    /**
     * Prefixes of fully qualified class names that should be considered for
     * re-instrumentation. Any loaded, modifiable class whose name starts with
     * one of these values will be re-transformed by the JUnit extension.
     */
    String[] value() default {};
}
