package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import io.github.dimkich.integration.testing.message.MessageConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Target(TYPE)
@Retention(RUNTIME)
@ExtendWith(JunitExtension.class)
@Import({MessageConfig.class, KafkaConfig.class})
@TestPropertySource("classpath:${integration.testing.environment:real}-kafka.properties")
public @interface EnableTestKafka {
}
