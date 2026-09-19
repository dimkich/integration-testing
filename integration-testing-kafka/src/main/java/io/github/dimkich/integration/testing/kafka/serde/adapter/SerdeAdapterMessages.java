package io.github.dimkich.integration.testing.kafka.serde.adapter;

/**
 * Remediation hints passed to
 * {@link io.github.dimkich.integration.testing.serde.TestSerdeSerializer#assertAcceptsContext}
 * by Kafka adapters. Kept here (not in the transport-agnostic core) because the
 * text is Kafka-specific.
 */
public final class SerdeAdapterMessages {

    private SerdeAdapterMessages() {
    }

    public static final String RECORD_LEVEL_PROVIDER_HINT =
            "use a record-level provider (e.g. spring-json) or implement "
                    + "KafkaRecordSerializer / KafkaRecordDeserializer directly";

    public static final String CONTEXT_FREE_HEADER_HINT =
            "header adapters accept only context-free providers; for context-aware "
                    + "serialization use a record-level provider (e.g. spring-json)";
}