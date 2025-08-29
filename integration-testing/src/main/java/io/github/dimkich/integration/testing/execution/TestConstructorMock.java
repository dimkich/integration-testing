package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
@Repeatable(TestConstructorMock.List.class)
public @interface TestConstructorMock {
    String name() default "";

    Class<?> mockClass() default Null.class;

    String mockClassName() default "";

    String[] methods() default {};

    boolean spy() default false;

    boolean cloneArgsAndResult() default false;

    @Inherited
    @Target(TYPE)
    @Retention(RUNTIME)
    @Documented
    @interface List {
        TestConstructorMock[] value();
    }
}
