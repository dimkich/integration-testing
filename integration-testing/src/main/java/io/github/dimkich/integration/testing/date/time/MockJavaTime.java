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
 * For tests that use Testcontainers, you can apply deterministic time to Docker containers
 * via {@link #dockerImages()}: specify regex patterns for image names that should receive
 * libfaketime instrumentation so that in-container time matches the mocked Java time.
 * </p>
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

    /**
     * Regex patterns for Testcontainers Docker image names that should have libfaketime
     * instrumentation applied, enabling deterministic time inside those containers.
     * <p>
     * When non-empty, integration-testing configures matching containers (via
     * {@link LibFakeTimeSetUp}) so that the OS-level "current time" inside the container
     * is controlled by the test framework, aligning with the mocked Java Time API.
     * </p>
     * <p>
     * Each value is a regex pattern matched against the full image name. For example:
     * </p>
     * <ul>
     *     <li>{@code "redis.*"} – applies libfaketime to Redis containers;</li>
     *     <li>{@code "postgres.*|mysql.*"} – applies to both Postgres and MySQL images.</li>
     * </ul>
     *
     * @see LibFakeTimeSetUp
     * @see LibFakeTimeTracker
     */
    String[] dockerImages() default {};
}
