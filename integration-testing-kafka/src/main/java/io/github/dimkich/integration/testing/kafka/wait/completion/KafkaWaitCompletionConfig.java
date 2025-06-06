package io.github.dimkich.integration.testing.kafka.wait.completion;

import io.github.dimkich.integration.testing.ConditionalOnRealServices;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;

import java.util.Arrays;

@Order
@Configuration
@RequiredArgsConstructor
@Import({KafkaTemplateInterceptor.class, MessageCounter.class, KafkaWaitCompletion.class})
@ConditionalOnRealServices
@ConditionalOnProperty(name = "integration.testing.wait.completion.kafkaStandardTask", havingValue = "true")
public class KafkaWaitCompletionConfig implements BeanPostProcessor {
    private final ConfigurableBeanFactory beanFactory;
    private final KafkaWaitCompletion kafkaWaitCompletion;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
            factory.setBatchInterceptor(kafkaWaitCompletion.createBatchInterceptor(null));
            factory.setRecordInterceptor(kafkaWaitCompletion.createRecordInterceptor(null));

            ConsumerRebalanceListener listener = factory.getContainerProperties().getConsumerRebalanceListener();
            ConsumersStartListener consumersStartListener = new ConsumersStartListener();
            if (listener instanceof ConsumerAwareRebalanceListener c) {
                consumersStartListener.setConsumerAwareRebalanceListener(c);
            } else {
                consumersStartListener.setConsumerRebalanceListener(listener);
            }
            factory.getContainerProperties().setConsumerRebalanceListener(consumersStartListener);
            kafkaWaitCompletion.setConsumersStartListener(consumersStartListener);
        }
        addKafkaListener(AnnotatedElementUtils.findMergedAnnotation(bean.getClass(), KafkaListener.class));
        addKafkaListeners(AnnotationUtils.findAnnotation(bean.getClass(), KafkaListeners.class));
        MethodIntrospector.selectMethods(bean.getClass(), (MethodIntrospector.MetadataLookup<Object>) method -> {
            addKafkaListener(method.getAnnotation(KafkaListener.class));
            addKafkaListeners(method.getAnnotation(KafkaListeners.class));
            return null;
        });
        return bean;
    }

    private void addKafkaListeners(KafkaListeners kafkaListeners) {
        if (kafkaListeners == null) {
            return;
        }
        Arrays.stream(kafkaListeners.value()).forEach(this::addKafkaListener);
    }

    private void addKafkaListener(KafkaListener kafkaListener) {
        if (kafkaListener == null) {
            return;
        }
        for (String topicExpression : kafkaListener.topics()) {
            String topic = (String) beanFactory.getBeanExpressionResolver().evaluate(
                    this.beanFactory.resolveEmbeddedValue(topicExpression),
                    new BeanExpressionContext(beanFactory, null)
            );
            kafkaWaitCompletion.addListener(topic);
        }
    }
}
