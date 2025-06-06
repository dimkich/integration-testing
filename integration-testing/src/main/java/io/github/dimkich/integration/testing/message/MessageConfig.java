package io.github.dimkich.integration.testing.message;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(value = "integration.testing.message.enabled", havingValue = "true")
@Import(TestMessagePoller.class)
public class MessageConfig {
}
