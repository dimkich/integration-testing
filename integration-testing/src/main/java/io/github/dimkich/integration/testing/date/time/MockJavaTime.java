package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Used to mock all java time for tests (almost, some classes were skipped, because they have nothing to do with java
 * time api).
 * If there are some calls to {@link java.lang.System#currentTimeMillis()} in your code or in code of your libraries,
 * you can set classes and/or packages in {@link #value()} to mock them too. Search pattern is "class name start with".
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JunitExtension.class)
public @interface MockJavaTime {
    /**
     *  Classes and/or packages where {@link java.lang.System#currentTimeMillis()} calls must be mocked for testing
     */
    String[] value() default {};
}
