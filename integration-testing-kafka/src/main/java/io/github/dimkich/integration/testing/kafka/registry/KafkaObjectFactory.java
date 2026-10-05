package io.github.dimkich.integration.testing.kafka.registry;

import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.config.TopicProperties;
import io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Creates {@link KafkaTopicMetadata} from {@link TopicProperties}: compiles the
 * excluded-fields tree and defers serde resolution until a serializer or deserializer is
 * actually requested. The record converter is always assembled by
 * {@link KafkaRecordSerdeFactory} from the {@code value}/{@code key}/{@code headers}
 * components; there is no record-level converter. Terminal defaults are applied here,
 * after the whole inheritance chain has been merged.
 */
@Slf4j
@SuppressWarnings("rawtypes")
public class KafkaObjectFactory {
    private final FieldExclusionProcessor fieldExclusionProcessor;
    private final KafkaRecordSerdeFactory recordSerdeFactory;
    private final PropertyInheritanceMerger merger;

    /**
     * Creates the factory.
     *
     * @param fieldExclusionProcessor compiles the excluded-fields configuration
     * @param recordSerdeFactory      assembles record converters from components
     * @param merger                  applies the default source to terminal record
     *                                configurations without an explicit one
     */
    public KafkaObjectFactory(FieldExclusionProcessor fieldExclusionProcessor,
                              KafkaRecordSerdeFactory recordSerdeFactory,
                              PropertyInheritanceMerger merger) {
        this.fieldExclusionProcessor = fieldExclusionProcessor;
        this.recordSerdeFactory = recordSerdeFactory;
        this.merger = merger;
    }

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

    private TestSerdeConverter<KafkaRecord, ProducerRecord, TestSerdeContext> resolveRecordSerializer(
            RecordProperties props) {
        log.debug("Kafka serde: assembling serializer from value/key/headers components");
        return recordSerdeFactory.createSerializer(withDefaults(props));
    }

    private TestSerdeConverter<ConsumerRecord, KafkaRecord, TestSerdeContext> resolveRecordDeserializer(
            RecordProperties props) {
        log.debug("Kafka serde: assembling deserializer from value/key/headers components");
        return recordSerdeFactory.createDeserializer(withDefaults(props));
    }

    private RecordProperties withDefaults(RecordProperties props) {
        RecordProperties effective = props != null ? props : new RecordProperties();
        effective.applyDefaultSource(merger);
        return effective;
    }
}
