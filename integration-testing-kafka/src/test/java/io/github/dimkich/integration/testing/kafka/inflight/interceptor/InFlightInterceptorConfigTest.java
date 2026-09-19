package io.github.dimkich.integration.testing.kafka.inflight.interceptor;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class InFlightInterceptorConfigTest {

    @Test
    void consumerAcceptsBootstrapServersAsList() {
        try (InFlightConsumerInterceptor interceptor = new InFlightConsumerInterceptor()) {
            assertThatCode(() -> interceptor.configure(Map.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("host1:9092", "host2:9092"),
                    ConsumerConfig.GROUP_ID_CONFIG, "group")))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void consumerAcceptsBootstrapServersAsString() {
        try (InFlightConsumerInterceptor interceptor = new InFlightConsumerInterceptor()) {
            assertThatCode(() -> interceptor.configure(Map.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "host1:9092,host2:9092",
                    ConsumerConfig.GROUP_ID_CONFIG, "group")))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void producerAcceptsBootstrapServersAsList() {
        try (InFlightProducerInterceptor interceptor = new InFlightProducerInterceptor()) {
            assertThatCode(() -> interceptor.configure(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("host1:9092", "host2:9092"))))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void producerAcceptsBootstrapServersAsString() {
        try (InFlightProducerInterceptor interceptor = new InFlightProducerInterceptor()) {
            assertThatCode(() -> interceptor.configure(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "host1:9092,host2:9092")))
                    .doesNotThrowAnyException();
        }
    }
}
