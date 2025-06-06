package io.github.dimkich.integration.testing;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;

@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@PropertySource(value = "classpath:integration-testing.properties")
public class DefaultsPropertiesLoader {
}
