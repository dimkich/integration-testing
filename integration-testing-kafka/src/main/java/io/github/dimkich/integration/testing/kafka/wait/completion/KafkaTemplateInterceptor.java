package io.github.dimkich.integration.testing.kafka.wait.completion;

import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("kafkaTemplateInterceptor")
public class KafkaTemplateInterceptor implements MethodInterceptor {
    private final MessageCounter messageCounter;

    @Nullable
    @Override
    public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        if (args[0] instanceof String topic) {
            messageCounter.messageSent(topic);
        } else if (args[0] instanceof Message<?> message) {
            messageCounter.messageSent((String) message.getHeaders().get(KafkaHeaders.TOPIC));
        } else if (args[0] instanceof ProducerRecord<?, ?> record) {
            messageCounter.messageSent(record.topic());
        } else {
            throw new RuntimeException("Unknown message type");
        }
        return invocation.proceed();
    }
}
