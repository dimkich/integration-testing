package io.github.dimkich.integration.testing.kafka.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.mockito.Mockito;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.adapter.BatchMessagingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.HandlerAdapter;
import org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.converter.MessagingMessageConverter;

import java.lang.reflect.Method;
import java.util.List;

public class KafkaListenerInvoker {
    private final RecordMessagingMessageListenerAdapter<Object, Object> messageListener;
    private final BatchMessagingMessageListenerAdapter<Object, Object> batchListener;
    private final Acknowledgment acknowledgment = Mockito.mock(Acknowledgment.class);
    private final Consumer<Object, Object> consumer = Mockito.mock(Consumer.class);

    public KafkaListenerInvoker(Object bean, Method method) {
        messageListener = new RecordMessagingMessageListenerAdapter<>(bean, method);
        batchListener = new BatchMessagingMessageListenerAdapter<>(bean, method);
        MessagingMessageConverter messageConverter = new MessagingMessageConverter();
        messageConverter.setHeaderMapper(new KafkaHeaderMapper());
        messageListener.setMessageConverter(messageConverter);
        batchListener.setMessageConverter(messageConverter);
    }

    public void invoke(ConsumerRecord<Object, Object> consumerRecord) {
        try {
            messageListener.onMessage(consumerRecord, acknowledgment, consumer);
        } catch (ListenerExecutionFailedException e) {
            batchListener.onMessage(List.of(consumerRecord), acknowledgment, consumer);
        }
    }

    public void setHandlerMethod(HandlerAdapter handlerMethod) {
        messageListener.setHandlerMethod(handlerMethod);
        batchListener.setHandlerMethod(handlerMethod);
    }
}
