package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JunitExtension.class)
@Repeatable(TestCaseStaticMock.List.class)
public @interface TestCaseStaticMock {
    String name();

    Class<?> mockClass();

    String[] methods() default {};

    boolean spy() default false;

    @Target(TYPE)
    @Retention(RUNTIME)
    @Documented
    @interface List {
        TestCaseStaticMock[] value();
    }
}
