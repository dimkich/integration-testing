package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.postgresql.config.PostgresConfig;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enables the trigger based PostgreSQL delta storage for a test class.
 * <p>
 * Inherits {@link IntegrationTesting} (which initializes the core framework) and imports
 * {@link PostgresConfig}, which registers the storage components and a
 * {@link SQLDataStorageService} bean for every connection declared under
 * {@code integration.testing.postgresql.connections}.
 * <p>
 * The annotation also imports {@code postgresql.properties}, which enables the embedded PostgreSQL
 * container by default. Configure at least one connection before using the annotation.
 *
 * @see PostgresConfig
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@TestPropertySource("classpath:postgresql.properties")
@Import(PostgresConfig.class)
public @interface EnableTestPostgreSQL {
}
