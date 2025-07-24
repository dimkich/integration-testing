package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(TestCaseStaticMock.List.class)
public @interface TestCaseStaticMock {
    String name();

    Class<?> mockClass();

    String[] methods() default {};

    boolean spy() default false;

    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @Documented
    @interface List {
        TestCaseStaticMock[] value();
    }
}
