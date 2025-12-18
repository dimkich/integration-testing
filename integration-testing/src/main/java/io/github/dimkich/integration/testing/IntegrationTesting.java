package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Enables the integration-testing framework for a JUnit 5 test class.
 * <p>
 * Apply this annotation on a test class to register the {@link JunitExtension},
 * which initializes and manages the integration test environment.
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JunitExtension.class)
public @interface IntegrationTesting {
}
