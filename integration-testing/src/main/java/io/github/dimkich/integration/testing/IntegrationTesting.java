package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables integration-testing support for a JUnit 5 test class.
 * <p>
 * This meta-annotation registers the {@link JunitExtension} and imports
 * {@link IntegrationTestConfig}, which initialize and manage the integration
 * test environment.
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JunitExtension.class)
@Import(IntegrationTestConfig.class)
public @interface IntegrationTesting {
}
