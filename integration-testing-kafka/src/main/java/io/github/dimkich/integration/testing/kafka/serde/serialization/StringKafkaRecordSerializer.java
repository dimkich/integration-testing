package io.github.dimkich.integration.testing.kafka.serde.serialization;

import io.github.dimkich.integration.testing.kafka.serde.StringKafkaSerdeDefaults;

/**
 * Record serializer with string key, value and plain-style headers.
 * <p>
 * Default for topics without explicit serde configuration.
 */
public class StringKafkaRecordSerializer extends SpringKafkaRecordSerializer<String, String> {

    /**
     * Creates the serializer with string defaults.
     */
    public StringKafkaRecordSerializer() {
        super(
                StringKafkaSerdeDefaults.KEY_VALUE_SERIALIZER,
                StringKafkaSerdeDefaults.KEY_VALUE_SERIALIZER,
                StringKafkaSerdeDefaults.CORE_HEADER_SERIALIZER
        );
    }
}
