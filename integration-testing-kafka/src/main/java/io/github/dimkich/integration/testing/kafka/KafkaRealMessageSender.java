package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.kafka.kafka.KafkaListenerInvoker;
import io.github.dimkich.integration.testing.message.MessageDto;
import io.github.dimkich.integration.testing.message.TestMessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component("kafkaTestMessageSender")
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnMissingBean(type = "kafkaTestMessageSender")
public class KafkaRealMessageSender implements TestMessageSender {
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    @Autowired(required = false)
    @Qualifier("kafkaTransactionManager")
    private final PlatformTransactionManager transactionManager;
    private final Map<String, List<KafkaListenerInvoker>> topicToHandlers;

    @Override
    public boolean canSend(MessageDto<?> message) {
        if (message.getHeaders() == null || message.getHeaders().getTopic() == null) {
            return false;
        }
        return topicToHandlers.containsKey(message.getHeaders().getTopic());
    }

    @Override
    public void sendInboundMessage(MessageDto<?> messageDto) {
        TransactionStatus status = null;
        if (transactionManager != null) {
            status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        }
        try {
            Message<?> kafkaMessage = MessageBuilder
                    .withPayload(messageDto.getPayload())
                    .setHeader(KafkaHeaders.TOPIC, messageDto.getHeaders().getTopic())
                    .setHeader(KafkaHeaders.KEY, messageDto.getHeaders().getKey())
                    .setHeader(MessageDto.TEST_INBOUND_MESSAGE, true)
                    .build();
            kafkaTemplate.send(kafkaMessage);
            if (transactionManager != null) {
                transactionManager.commit(status);
            }
        } catch (Exception e) {
            if (transactionManager != null) {
                transactionManager.rollback(status);
            }
            throw e;
        }
    }
}
