package io.github.dimkich.integration.testing.web;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JunitExtension.class)
@Repeatable(TestRestTemplate.List.class)
public @interface TestRestTemplate {
    String beanName();

    String basePath() default "";

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        TestRestTemplate[] value();
    }
}
