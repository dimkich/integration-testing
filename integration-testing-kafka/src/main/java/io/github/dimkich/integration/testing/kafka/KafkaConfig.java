package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.ConditionalOnMockedServices;
import io.github.dimkich.integration.testing.ConditionalOnRealServices;
import io.github.dimkich.integration.testing.kafka.kafka.KafkaTopicToHandlersConfig;
import io.github.dimkich.integration.testing.kafka.wait.completion.KafkaWaitCompletionConfig;
import io.github.dimkich.integration.testing.message.MessageDto;
import io.github.dimkich.integration.testing.message.TestMessagePoller;
import io.github.dimkich.integration.testing.message.TestMessageSender;
import io.github.sugarcubes.cloner.Cloner;
import io.github.sugarcubes.cloner.Cloners;
import lombok.RequiredArgsConstructor;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import org.springframework.kafka.config.KafkaListenerConfigUtils;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.CompletableFuture;

@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@Import({KafkaTopicToHandlersConfig.class, KafkaWaitCompletionConfig.class})
public class KafkaConfig {

    @Configuration
    @ConditionalOnRealServices
    @Import(KafkaRealMessageSender.class)
    public static class RealConfig {
    }

    @Configuration
    @RequiredArgsConstructor
    @ConditionalOnMockedServices
    @Import({KafkaMockMessageSender.class})
    public static class MockConfig {

        private final TestMessagePoller poller;

        @Qualifier("kafkaTestMessageSender")
        private final TestMessageSender sender;

        private final Cloner cloner = Cloners.builder().build();

        @Bean
        KafkaTemplate<?, ?> kafkaTemplate() {
            CompletableFuture<?> completableFuture = Mockito.mock(CompletableFuture.class);
            KafkaTemplate<?, ?> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
            Mockito.when(kafkaTemplate.send(ArgumentMatchers.any(org.springframework.messaging.Message.class))).thenAnswer(invocation -> {
                org.springframework.messaging.Message<Object> message = invocation.getArgument(0);
                MessageDto<Object> messageDto = KafkaMessageMapper.toMessageDto(message);
                messageDto.setPayload(cloner.clone(messageDto.getPayload()));
                handleMessage(messageDto);
                return completableFuture;
            });
            Mockito.when(kafkaTemplate.send(ArgumentMatchers.any(String.class), ArgumentMatchers.any(), ArgumentMatchers.any())).thenAnswer(invocation -> {
                MessageDto<Object> message = new MessageDto<>();
                message.getHeaders().setTopic(invocation.getArgument(0));
                message.getHeaders().setKey(cloner.clone(invocation.getArgument(1)));
                message.setPayload(cloner.clone(invocation.getArgument(2)));
                handleMessage(message);
                return completableFuture;
            });
            Mockito.when(kafkaTemplate.send(ArgumentMatchers.any(String.class), ArgumentMatchers.any())).thenAnswer(invocation -> {
                MessageDto<Object> message = new MessageDto<>();
                message.getHeaders().setTopic(invocation.getArgument(0));
                message.setPayload(cloner.clone(invocation.getArgument(1)));
                handleMessage(message);
                return completableFuture;
            });
            return kafkaTemplate;
        }

        @Bean(name = KafkaListenerConfigUtils.KAFKA_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
        KafkaListenerAnnotationBeanPostProcessor<?, ?> kafkaListenerAnnotationBeanPostProcessor() {
            KafkaListenerAnnotationBeanPostProcessor<?, ?> processor = Mockito.mock(KafkaListenerAnnotationBeanPostProcessor.class);
            Mockito.doAnswer(invocation -> invocation.getArgument(0)).when(processor).postProcessBeforeInitialization(ArgumentMatchers.any(), ArgumentMatchers.any());
            Mockito.doAnswer(invocation -> invocation.getArgument(0)).when(processor).postProcessAfterInitialization(ArgumentMatchers.any(), ArgumentMatchers.any());
            return processor;
        }

        @Bean(name = KafkaListenerConfigUtils.KAFKA_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)
        KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry() {
            return Mockito.mock(KafkaListenerEndpointRegistry.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_MOCKS));
        }

        private void handleMessage(MessageDto<?> message) {
            poller.putMessage(message);
            if (sender.canSend(message)) {
                sender.sendInboundMessage(message);
            }
        }

        @Bean
        @ConditionalOnProperty(name = "spring.kafka.producer.transaction-id-prefix")
        public KafkaTransactionManager<?, ?> kafkaTransactionManager() {
            KafkaTransactionManager<?, ?> kafkaTransactionManager = Mockito.mock(KafkaTransactionManager.class);
            ProducerFactory<?, ?> producerFactory = Mockito.mock(ProducerFactory.class);
            Mockito.doReturn(producerFactory).when(kafkaTransactionManager).getProducerFactory();
            return kafkaTransactionManager;
        }

        @Bean
        @ConditionalOnClass({JpaBaseConfiguration.class, TransactionManagerCustomizers.class})
        public PlatformTransactionManager transactionManager(
                ObjectProvider<JpaBaseConfiguration> jpaBaseConfiguration,
                ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
            JpaBaseConfiguration configuration = jpaBaseConfiguration.getIfAvailable();
            if (configuration != null) {
                return configuration.transactionManager(transactionManagerCustomizers);
            }
            return null;
        }
    }
}
