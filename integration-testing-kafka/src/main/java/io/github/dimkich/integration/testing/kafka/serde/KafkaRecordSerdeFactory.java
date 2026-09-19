package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaRecordDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.SpringKafkaRecordDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaHeaderSerializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaRecordSerializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.SpringKafkaRecordSerializer;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import static io.github.dimkich.integration.testing.kafka.serde.StringKafkaSerdeDefaults.KEY_VALUE_DESERIALIZER;
import static io.github.dimkich.integration.testing.kafka.serde.StringKafkaSerdeDefaults.KEY_VALUE_SERIALIZER;

/**
 * Single assembly point for {@link KafkaRecordSerializer} and
 * {@link KafkaRecordDeserializer} from key, value, and header components.
 * <p>
 * The default header serializer/deserializer is provided by the caller
 * (Core-style for plain topics, Spring-style for spring-json/spring-xml).
 */
@RequiredArgsConstructor
public class KafkaRecordSerdeFactory {

    private final SerdeManager serdeManager;

    /**
     * Assembles a record serializer from the configured key and header parts; the
     * value serializer is supplied by the provider.
     *
     * @param props the record-level configuration, may be {@code null}
     * @param valueSerializer the serializer for the message value
     * @param defaultHeaderSerializer the header serializer used when headers are not
     *                                configured explicitly
     * @return the assembled record serializer
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public KafkaRecordSerializer createSerializer(RecordProperties props,
                                                  Serializer<Object> valueSerializer,
                                                  KafkaHeaderSerializer defaultHeaderSerializer) {
        SerdeProperties keyProps = props != null ? props.getKey() : null;

        Serializer keySerializer = serdeManager.resolveAndAdaptSerializer(
                keyProps, Serializer.class, () -> KEY_VALUE_SERIALIZER);

        KafkaHeaderSerializer headerSerializer = serdeManager.resolveAndAdaptSerializer(
                props != null ? props.getHeaders() : null,
                KafkaHeaderSerializer.class,
                () -> defaultHeaderSerializer);

        return new SpringKafkaRecordSerializer<>(
                keySerializer,
                valueSerializer,
                headerSerializer
        );
    }

    /**
     * Assembles a record deserializer from the configured key and header parts; the
     * value deserializer is supplied by the provider.
     *
     * @param props the record-level configuration, may be {@code null}
     * @param valueDeserializer the deserializer for the message value
     * @param defaultHeaderDeserializer the header deserializer used when headers are
     *                                  not configured explicitly
     * @return the assembled record deserializer
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public KafkaRecordDeserializer createDeserializer(RecordProperties props,
                                                      Deserializer<Object> valueDeserializer,
                                                      KafkaHeaderDeserializer defaultHeaderDeserializer) {
        SerdeProperties keyProps = props != null ? props.getKey() : null;

        Deserializer keyDeserializer = serdeManager.resolveAndAdaptDeserializer(
                keyProps, Deserializer.class, () -> KEY_VALUE_DESERIALIZER);

        KafkaHeaderDeserializer headerDeserializer = serdeManager.resolveAndAdaptDeserializer(
                props != null ? props.getHeaders() : null,
                KafkaHeaderDeserializer.class,
                () -> defaultHeaderDeserializer);

        return new SpringKafkaRecordDeserializer<>(
                keyDeserializer,
                valueDeserializer,
                headerDeserializer
        );
    }
}
