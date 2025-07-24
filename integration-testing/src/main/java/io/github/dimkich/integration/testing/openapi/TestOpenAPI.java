package io.github.dimkich.integration.testing.openapi;

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
@Repeatable(TestOpenAPI.List.class)
public @interface TestOpenAPI {
    Class<?> apiClass();

    Class<?> errorResponseClass() default SpringErrorDto.class;

    String apiBeanName() default "";

    String restTemplateBeanName() default "";

    String basePath() default "";

    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        TestOpenAPI[] value();
    }
}
