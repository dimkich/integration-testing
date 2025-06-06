package io.github.dimkich.integration.testing.kafka.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.kafka.listener.adapter.HandlerAdapter;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicToHandlersConfig implements BeanPostProcessor {
    private final ConfigurableBeanFactory beanFactory;
    private final Map<String, List<KafkaListenerInvoker>> topicToHandlers = new ConcurrentHashMap<>();
    private DefaultMessageHandlerMethodFactory messageHandlerMethodFactory;

    @PostConstruct
    public void init() {
        messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        MessageConverter messageConverter = new GenericMessageConverter(new DefaultFormattingConversionService());
        messageHandlerMethodFactory.setMessageConverter(messageConverter);
        messageHandlerMethodFactory.afterPropertiesSet();
    }

    @Bean
    Map<String, List<KafkaListenerInvoker>> topicToHandlers() {
        return topicToHandlers;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        List<String> topics = new ArrayList<>();
        if (AnnotatedElementUtils.findMergedAnnotation(bean.getClass(), KafkaListener.class) != null) {
            throw new RuntimeException("Annotation KafkaListener on class level is not supported");
        }
        if (AnnotatedElementUtils.findMergedAnnotation(bean.getClass(), KafkaListeners.class) != null) {
            throw new RuntimeException("Annotation KafkaListeners on class level is not supported");
        }
        MethodIntrospector.selectMethods(bean.getClass(), (MethodIntrospector.MetadataLookup<Object>) method -> {
            topics.clear();
            addKafkaListener(topics, method.getAnnotation(KafkaListener.class));
            addKafkaListeners(topics, method.getAnnotation(KafkaListeners.class));
            if (!topics.isEmpty()) {
                KafkaListenerInvoker invoker = new KafkaListenerInvoker(bean, method);
                invoker.setHandlerMethod(new HandlerAdapter(
                        messageHandlerMethodFactory.createInvocableHandlerMethod(bean, method)));
                for (String topic : topics) {
                    List<KafkaListenerInvoker> list = topicToHandlers.computeIfAbsent(topic, t -> new ArrayList<>());
                    list.add(invoker);
                }
            }
            return null;
        });
        return bean;
    }

    private void addKafkaListeners(List<String> topics, KafkaListeners kafkaListeners) {
        if (kafkaListeners == null) {
            return;
        }
        Arrays.stream(kafkaListeners.value()).forEach(kafkaListener -> addKafkaListener(topics, kafkaListener));
    }

    private void addKafkaListener(List<String> topics, KafkaListener kafkaListener) {
        if (kafkaListener == null) {
            return;
        }
        for (String topicExpression : kafkaListener.topics()) {
            String topic = (String) beanFactory.getBeanExpressionResolver().evaluate(
                    this.beanFactory.resolveEmbeddedValue(topicExpression),
                    new BeanExpressionContext(beanFactory, null)
            );
            topics.add(topic);
        }
    }
}
