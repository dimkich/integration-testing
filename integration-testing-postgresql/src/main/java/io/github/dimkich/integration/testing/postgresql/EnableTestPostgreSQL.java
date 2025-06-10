package io.github.dimkich.integration.testing.postgresql;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@TestPropertySource("classpath:postgresql.properties")
@ImportAutoConfiguration(classes = PostgresqlDataStorageFactory.class)
public @interface EnableTestPostgreSQL {
}
