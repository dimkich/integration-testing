package io.github.dimkich.integration.testing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(name = "integration.testing.environment", havingValue = "real", matchIfMissing = true)
public @interface ConditionalOnRealServices {
}
