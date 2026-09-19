package io.github.dimkich.integration.testing.kafka.uncommitted;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.SeekUtils;

@Configuration
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class SutErrorHandlingExamples {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template));
        handler.setCommitRecovered(true);
        return handler;
    }

    @Bean
    public DefaultAfterRollbackProcessor<Object, Object> rollbackProcessor(KafkaTemplate<Object, Object> template) {
        return new DefaultAfterRollbackProcessor<>(
                new DeadLetterPublishingRecoverer(template),
                SeekUtils.DEFAULT_BACK_OFF,
                template,
                true);
    }
}
