package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
public @interface TestBeanMocks {
    Class<?>[] mockClasses() default {};
    String[] mockNames() default {};
    Class<?>[] spyClasses() default {};
    String[] spyNames() default {};
}
