package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.kafka.serde.deserialization.CoreKafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.CoreKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaHeaderSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Default key/value serde for Kafka records. Header mapping is handled
 * by {@link org.springframework.kafka.support.DefaultKafkaHeaderMapper}
 * inside {@link KafkaRecordSerdeFactory}.
 */
public final class StringKafkaSerdeDefaults {

    private StringKafkaSerdeDefaults() {
    }

    public static final Serializer<String> KEY_VALUE_SERIALIZER =
            new org.apache.kafka.common.serialization.StringSerializer();

    public static final Deserializer<String> KEY_VALUE_DESERIALIZER =
            new org.apache.kafka.common.serialization.StringDeserializer();

    public static final KafkaHeaderSerializer CORE_HEADER_SERIALIZER =
            new CoreKafkaHeaderSerializer(new io.github.dimkich.integration.testing.serde.impl.StringSerializer());

    public static final KafkaHeaderDeserializer CORE_HEADER_DESERIALIZER =
            new CoreKafkaHeaderDeserializer(new io.github.dimkich.integration.testing.serde.impl.StringDeserializer());
}
