package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to mock all java time for tests (almost, some classes were skipped, because they have nothing to do with java
 * time api).
 * If there are some calls to {@link java.lang.System#currentTimeMillis()} in your code or in code of your libraries,
 * you can set classes and/or packages in {@link #value()} to mock them too. Search pattern is "class name start with".
 */
@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@IntegrationTesting
public @interface MockJavaTime {
    /**
     *  Classes and/or packages where {@link java.lang.System#currentTimeMillis()} calls must be mocked for testing
     */
    String[] value() default {};
}
