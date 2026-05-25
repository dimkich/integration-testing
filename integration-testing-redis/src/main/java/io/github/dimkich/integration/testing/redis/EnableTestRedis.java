package io.github.dimkich.integration.testing.redis;

import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.redis.config.RedisConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enables Redis integration testing support for a test class.
 * <p>
 * This annotation performs the following functions:
 * <ul>
 *     <li>Inherits the {@link io.github.dimkich.integration.testing.IntegrationTesting}
 *         meta-annotation, which initializes the core integration testing infrastructure
 *         (e.g., {@link io.github.dimkich.integration.testing.execution.junit.JunitExtension}).</li>
 *     <li>Imports the {@link io.github.dimkich.integration.testing.redis.config.RedisConfig}
 *         configuration, which sets up the Redis testing infrastructure (codecs, data schemas,
 *         replication, and in-memory storage).</li>
 *     <li>Loads Redis configuration properties from the {@code redis.properties}
 *         classpath resource into the Spring context.</li>
 * </ul>
 * <p>
 * This annotation should be used on test classes that require interaction with Redis
 * within an integration testing context.
 *
 * @see io.github.dimkich.integration.testing.redis.config.RedisConfig
 * @see io.github.dimkich.integration.testing.IntegrationTesting
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Import({RedisConfig.class})
@TestPropertySource("classpath:redis.properties")
public @interface EnableTestRedis {
}