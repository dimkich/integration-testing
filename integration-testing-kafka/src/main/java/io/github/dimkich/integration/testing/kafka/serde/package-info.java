/**
 * Kafka serialization subsystem: assembles record serializers and deserializers from
 * key, value and header parts and bridges the core serde contracts to native Kafka
 * types.
 *
 * <h2>Interfaces</h2>
 * <ul>
 *   <li>{@link io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaRecordSerializer}
 *       and
 *       {@link io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaRecordDeserializer} —
 *       record-level contracts operating on {@code KafkaRecord} and raw byte payloads;</li>
 *   <li>{@link io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaHeaderSerializer}
 *       and
 *       {@link io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaHeaderDeserializer} —
 *       conversion between message header maps and native Kafka {@code Headers};</li>
 *   <li>{@link io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext} —
 *       transport context exposing the topic and headers to context-aware serializers.</li>
 * </ul>
 *
 * <h2>Assembly</h2>
 * <p>{@link io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory}
 * combines the configured key and header parts with the provider-supplied value serde.
 * The {@code adapter} package bridges core {@code TestSerdeSerializer} and
 * {@code TestSerdeDeserializer} to native Kafka types, {@code provider} contains the
 * record-level {@code spring-json} and {@code spring-xml} providers, and
 * {@code serialization}/{@code deserialization} contain the concrete implementations.</p>
 */
package io.github.dimkich.integration.testing.kafka.serde;
