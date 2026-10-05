package io.github.dimkich.integration.testing.message;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TestMessages.class)
public class MessageConfig {
}
