package io.github.dimkich.integration.testing.kafka.registry;

import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.config.TopicProperties;
import io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaRecordDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.StringKafkaRecordDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaRecordSerializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.StringKafkaRecordSerializer;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import static io.github.dimkich.integration.testing.kafka.serde.StringKafkaSerdeDefaults.*;

/**
 * Creates {@link KafkaTopicMetadata} from {@link TopicProperties}: compiles the
 * excluded-fields tree and defers serde assembly until a serializer or deserializer is
 * actually requested.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaObjectFactory {
    private final FieldExclusionProcessor fieldExclusionProcessor;
    private final SerdeManager serdeManager;
    private final KafkaRecordSerdeFactory recordSerdeFactory;

    /**
     * Creates metadata for the given configuration, using defaults when the
     * configuration is absent.
     *
     * @param props the topic configuration, may be {@code null}
     * @return the topic metadata
     */
    public KafkaTopicMetadata createMetadata(TopicProperties props) {
        TopicProperties topicProps = props != null ? props : new TopicProperties();
        return new KafkaTopicMetadata(
                () -> resolveRecordDeserializer(topicProps.getDeserializer()),
                () -> resolveRecordSerializer(topicProps.getSerializer()),
                topicProps.isIgnore(),
                topicProps.isIgnoreInbound(),
                fieldExclusionProcessor.compile(topicProps.getExcludedFields())
        );
    }

    @SuppressWarnings("unchecked")
    private KafkaRecordSerializer resolveRecordSerializer(RecordProperties props) {
        if (props == null) {
            return new StringKafkaRecordSerializer();
        }
        return serdeManager.resolveAndAdaptSerializer(props, KafkaRecordSerializer.class, () -> {
            log.debug("Kafka serde: no record-level serializer type/beanRef, assembling from value/key parts");
            Serializer<Object> valueSer = serdeManager.resolveAndAdaptSerializer(
                    props.getValue(), Serializer.class, () -> KEY_VALUE_SERIALIZER);
            return recordSerdeFactory.createSerializer(props, valueSer, CORE_HEADER_SERIALIZER);
        });
    }

    @SuppressWarnings("unchecked")
    private KafkaRecordDeserializer resolveRecordDeserializer(RecordProperties props) {
        if (props == null) {
            return new StringKafkaRecordDeserializer();
        }
        return serdeManager.resolveAndAdaptDeserializer(props, KafkaRecordDeserializer.class, () -> {
            log.debug("Kafka serde: no record-level deserializer type/beanRef, assembling from value/key parts");
            Deserializer<Object> valueDeser = serdeManager.resolveAndAdaptDeserializer(
                    props.getValue(), Deserializer.class, () -> KEY_VALUE_DESERIALIZER);
            return recordSerdeFactory.createDeserializer(props, valueDeser, CORE_HEADER_DESERIALIZER);
        });
    }
}
