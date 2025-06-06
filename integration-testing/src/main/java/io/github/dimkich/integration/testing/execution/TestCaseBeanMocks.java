package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JunitExtension.class)
public @interface TestCaseBeanMocks {
    Class<?>[] mockClasses() default {};
    String[] mockNames() default {};
    Class<?>[] spyClasses() default {};
    String[] spyNames() default {};
}
