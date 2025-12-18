package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enables deterministic mocking of the Java Time API for tests.
 * <p>
 * When a test class is annotated with {@code @MockJavaTime}, integration-testing configures
 * the underlying infrastructure so that calls to most classes from the Java Time API
 * (for example {@link java.time.Clock}, {@link java.time.Instant}, {@link java.time.LocalDateTime})
 * return predictable, test-controlled values instead of relying on the real system time.
 * </p>
 *
 * <p>
 * In addition, this annotation can also mock calls to {@link java.lang.System#currentTimeMillis()}
 * in user code and in third‑party libraries. To do this, you can specify a list of class or package
 * name prefixes via {@link #value()}. Each entry is interpreted as a
 * “class name starts with” pattern that is used to decide where
 * {@code currentTimeMillis()} invocations should be intercepted.
 * </p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @MockJavaTime({
 *     "com.example.myapp",          // whole package hierarchy
 *     "org.thirdparty.lib.Client"   // concrete class and its inner classes
 * })
 * public class MyTimeSensitiveTest {
 *     // ...
 * }
 * }</pre>
 *
 * <p>
 * Only classes that are relevant to time handling are mocked; some JDK time-related
 * types that do not affect the observable notion of “current time” may be left untouched.
 * </p>
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
public @interface MockJavaTime {
    /**
     * Class and/or package name prefixes for which calls to
     * {@link java.lang.System#currentTimeMillis()} should be mocked during tests.
     * <p>
     * Each value is treated as a “starts with” pattern on the fully-qualified class name.
     * For example:
     * </p>
     * <ul>
     *     <li>{@code "com.example"} – mocks all classes under the {@code com.example} package tree;</li>
     *     <li>{@code "com.example.service.TimeService"} – mocks that class and its inner classes.</li>
     * </ul>
     */
    String[] value() default {};
}
