package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.execution.hook.OnMethodExit;
import io.github.dimkich.integration.testing.kafka.uncommitted.KafkaForceCommitAction;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

@EnableTestKafka
@SpringBootTest(classes = KafkaTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@OnMethodExit(
        pointcut = "t.inherits('org.springframework.kafka.listener.CommonErrorHandler') && m.name('handleRemaining')",
        action = KafkaForceCommitAction.class)
public class KafkaInFlightTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("kafka.xml");
    }
}
