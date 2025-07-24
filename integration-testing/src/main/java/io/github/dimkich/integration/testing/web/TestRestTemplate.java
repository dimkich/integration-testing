package io.github.dimkich.integration.testing.web;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(TestRestTemplate.List.class)
public @interface TestRestTemplate {
    String beanName();

    String basePath() default "";

    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        TestRestTemplate[] value();
    }
}
