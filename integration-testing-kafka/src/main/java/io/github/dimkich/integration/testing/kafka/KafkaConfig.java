package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.ConditionalOnMockedServices;
import io.github.dimkich.integration.testing.ConditionalOnRealServices;
import io.github.dimkich.integration.testing.kafka.kafka.KafkaTopicToHandlersConfig;
import io.github.dimkich.integration.testing.kafka.wait.completion.KafkaWaitCompletionConfig;
import io.github.dimkich.integration.testing.message.MessageDto;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.mockito.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.util.Map;
import java.util.Properties;
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
    @Import({KafkaMockMessageSender.class, MockKafkaWaitCompletion.class})
    public static class MockConfig implements BeanFactoryPostProcessor {
        private static MockedConstruction<KafkaTemplate> kafkaTemplateMock;
        private static MockedConstruction<KafkaTransactionManager> kafkaTransactionManagerMock;
        private static MockedConstruction<KafkaListenerEndpointRegistry> kafkaListenerEndpointRegistryMock;
        private static MockedConstruction<KafkaListenerAnnotationBeanPostProcessor> kafkaListenerAnnotationBeanPostProcessorMock;
        private static MockedConstruction<KafkaAdmin> kafkaAdminMock;
        private static MockedStatic<AdminClient> adminClientStatic;
        private static MockedConstruction<KafkaAdminClient> kafkaAdminClientMock;

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            if (kafkaTemplateMock != null) {
                kafkaTemplateMock.close();
                kafkaTemplateMock = null;
            }
            kafkaTemplateMock = Mockito.mockConstruction(KafkaTemplate.class,
                    Mockito.withSettings().stubOnly(),
                    (mock, context) -> {
                        KafkaTemplate<?, ?> kafkaTemplate = (KafkaTemplate<?, ?>) mock;
                        CompletableFuture<?> completableFuture = Mockito.mock(CompletableFuture.class);
                        Mockito.when(kafkaTemplate.send(ArgumentMatchers.any(org.springframework.messaging.Message.class))).thenAnswer(invocation -> {
                            org.springframework.messaging.Message<Object> message = invocation.getArgument(0);
                            MessageDto<Object> messageDto = KafkaMessageMapper.toMessageDto(message);
                            messageDto.setPayload(messageDto.getPayload());
                            beanFactory.getBean(MockKafkaWaitCompletion.class).addMessage(messageDto);
                            return completableFuture;
                        });
                        Mockito.when(kafkaTemplate.send(ArgumentMatchers.any(String.class), ArgumentMatchers.any(), ArgumentMatchers.any())).thenAnswer(invocation -> {
                            MessageDto<Object> message = new MessageDto<>();
                            message.getHeaders().setTopic(invocation.getArgument(0));
                            message.getHeaders().setKey(invocation.getArgument(1));
                            message.setPayload(invocation.getArgument(2));
                            beanFactory.getBean(MockKafkaWaitCompletion.class).addMessage(message);
                            return completableFuture;
                        });
                        Mockito.when(kafkaTemplate.send(ArgumentMatchers.any(String.class), ArgumentMatchers.any())).thenAnswer(invocation -> {
                            MessageDto<Object> message = new MessageDto<>();
                            message.getHeaders().setTopic(invocation.getArgument(0));
                            message.setPayload(invocation.getArgument(1));
                            beanFactory.getBean(MockKafkaWaitCompletion.class).addMessage(message);
                            return completableFuture;
                        });
                    });

            if (kafkaTransactionManagerMock == null) {
                kafkaTransactionManagerMock = Mockito.mockConstruction(KafkaTransactionManager.class,
                        Mockito.withSettings().stubOnly(),
                        (mock, context) -> {
                            KafkaTransactionManager<?, ?> kafkaTransactionManager = (KafkaTransactionManager<?, ?>) mock;
                            ProducerFactory<?, ?> producerFactory = Mockito.mock(ProducerFactory.class);
                            Mockito.doReturn(producerFactory).when(kafkaTransactionManager).getProducerFactory();
                        });
            }
            if (kafkaListenerEndpointRegistryMock == null) {
                kafkaListenerEndpointRegistryMock = Mockito.mockConstruction(KafkaListenerEndpointRegistry.class,
                        Mockito.withSettings().stubOnly().defaultAnswer(Answers.RETURNS_MOCKS));
            }
            if (kafkaListenerAnnotationBeanPostProcessorMock == null) {
                kafkaListenerAnnotationBeanPostProcessorMock = Mockito.mockConstruction(
                        KafkaListenerAnnotationBeanPostProcessor.class,
                        Mockito.withSettings().stubOnly(),
                        (mock, context) -> {
                            KafkaListenerAnnotationBeanPostProcessor<?, ?> processor =
                                    (KafkaListenerAnnotationBeanPostProcessor<?, ?>) mock;
                            Mockito.doAnswer(invocation -> invocation.getArgument(0)).when(processor)
                                    .postProcessBeforeInitialization(ArgumentMatchers.any(), ArgumentMatchers.any());
                            Mockito.doAnswer(invocation -> invocation.getArgument(0)).when(processor)
                                    .postProcessAfterInitialization(ArgumentMatchers.any(), ArgumentMatchers.any());
                        });
            }
            if (kafkaAdminMock == null) {
                kafkaAdminMock = Mockito.mockConstruction(
                        KafkaAdmin.class,
                        Mockito.withSettings().defaultAnswer(Mockito.RETURNS_DEEP_STUBS).stubOnly()
                );
            }
            if (adminClientStatic == null) {
                adminClientStatic = Mockito.mockStatic(AdminClient.class);
                adminClientStatic.when(() -> AdminClient.create(Mockito.any(Properties.class)))
                        .thenReturn(Mockito.mock(AdminClient.class,
                                Mockito.withSettings()
                                        .defaultAnswer(Mockito.RETURNS_DEEP_STUBS).stubOnly()));
                adminClientStatic.when(() -> AdminClient.create(Mockito.any(Map.class)))
                        .thenReturn(Mockito.mock(AdminClient.class,
                                Mockito.withSettings()
                                        .defaultAnswer(Mockito.RETURNS_DEEP_STUBS).stubOnly()));
            }
            if (kafkaAdminClientMock == null) {
                kafkaAdminClientMock = Mockito.mockConstruction(
                        KafkaAdminClient.class,
                        Mockito.withSettings().defaultAnswer(Mockito.RETURNS_DEEP_STUBS).stubOnly()
                );
            }
        }

        @PreDestroy
        public void destroy() {
            kafkaTemplateMock.close();
            kafkaTemplateMock = null;
            kafkaTransactionManagerMock.close();
            kafkaTransactionManagerMock = null;
            kafkaListenerEndpointRegistryMock.close();
            kafkaListenerEndpointRegistryMock = null;
            kafkaListenerAnnotationBeanPostProcessorMock.close();
            kafkaListenerAnnotationBeanPostProcessorMock = null;
            kafkaAdminMock.close();
            kafkaAdminMock = null;
            adminClientStatic.close();
            adminClientStatic = null;
            kafkaAdminClientMock.close();
            kafkaAdminClientMock = null;
        }
    }
}
