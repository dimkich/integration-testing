package io.github.dimkich.integration.testing.openapi;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JunitExtension.class)
@Repeatable(TestOpenAPI.List.class)
public @interface TestOpenAPI {
    Class<?> apiClass();

    Class<?> errorResponseClass() default SpringErrorDto.class;

    String apiBeanName() default "";

    String restTemplateBeanName() default "";

    String basePath() default "";

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        TestOpenAPI[] value();
    }
}
