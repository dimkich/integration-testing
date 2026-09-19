package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import io.github.dimkich.integration.testing.kafka.serde.StringKafkaSerdeDefaults;

/**
 * Record deserializer with string key, value and plain-style headers.
 * <p>
 * Default for topics without explicit serde configuration.
 */
public class StringKafkaRecordDeserializer extends SpringKafkaRecordDeserializer<String, String> {

    /**
     * Creates the deserializer with string defaults.
     */
    public StringKafkaRecordDeserializer() {
        super(
                StringKafkaSerdeDefaults.KEY_VALUE_DESERIALIZER,
                StringKafkaSerdeDefaults.KEY_VALUE_DESERIALIZER,
                StringKafkaSerdeDefaults.CORE_HEADER_DESERIALIZER
        );
    }
}
