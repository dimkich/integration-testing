package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.SpringKafkaRecordDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.SpringKafkaRecordSerializer;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.util.MultiValueMap;

/**
 * Single assembly point for record converters from the {@code value}, {@code key} and
 * {@code headers} components. A record converter is always assembled from the components;
 * there is no record-level converter of its own.
 * <p>
 * The record-level {@link RecordProperties} fields are a base for all components: they are
 * merged into the components when the configuration is prepared (see
 * {@link RecordProperties#prepare}), and a record without its own source gets the
 * {@code string} default applied at the terminal level (see
 * {@link RecordProperties#applyDefaultSource}), so resolution is always strict. Every
 * component converter is resolved by the core serde pipeline; the headers component is a
 * {@code KafkaHeaderComponentProperties} (no binary envelope) and is resolved as an ordinary
 * {@code HEADERS} request.
 */
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
public class KafkaRecordSerdeFactory {

    private final SerdeManager serdeManager;

    public TestSerdeConverter<KafkaRecord, ProducerRecord, TestSerdeContext> createSerializer(RecordProperties props) {
        return new SpringKafkaRecordSerializer(
                resolve(props.getKey(), KafkaComponentRole.KEY, Object.class, byte[].class,
                        KafkaSerdeContext.class),
                resolve(props.getValue(), KafkaComponentRole.VALUE, Object.class, byte[].class,
                        KafkaSerdeContext.class),
                resolve(props.getHeaders(), KafkaComponentRole.HEADERS, MultiValueMap.class, Headers.class,
                        TestSerdeContext.class));
    }

    public TestSerdeConverter<ConsumerRecord, KafkaRecord, TestSerdeContext> createDeserializer(RecordProperties props) {
        return new SpringKafkaRecordDeserializer(
                resolve(props.getKey(), KafkaComponentRole.KEY, byte[].class, Object.class,
                        KafkaSerdeContext.class),
                resolve(props.getValue(), KafkaComponentRole.VALUE, byte[].class, Object.class,
                        KafkaSerdeContext.class),
                resolve(props.getHeaders(), KafkaComponentRole.HEADERS, Headers.class, MultiValueMap.class,
                        TestSerdeContext.class));
    }

    private <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> resolve(
            TestSerdeProperties component, KafkaComponentRole role, Class<I> inputClass, Class<O> outputClass,
            Class<C> contextClass) {
        return serdeManager.resolve(component, inputClass, outputClass, contextClass, role);
    }
}
