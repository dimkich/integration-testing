/**
 * Kafka serialization subsystem: assembles record converters from key, value and header
 * parts and bridges the core serde contracts to native Kafka types.
 *
 * <h2>Contracts</h2>
 * <ul>
 *   <li>record converters:
 *       {@link io.github.dimkich.integration.testing.serde.TestSerdeConverter} from
 *       {@code KafkaRecord} to {@code ProducerRecord<byte[], byte[]>} and back from
 *       {@code ConsumerRecord<byte[], byte[]>} to {@code KafkaRecord};</li>
 *   <li>header converters:
 *       {@link io.github.dimkich.integration.testing.serde.TestSerdeConverter} between message
 *       header maps and native Kafka {@code Headers};</li>
 *   <li>{@link io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext} —
 *       transport context exposing the topic and headers to context-aware serializers.</li>
 * </ul>
 *
 * <h2>Assembly</h2>
 * <p>{@link io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory}
 * assembles the record converter from the key, value and headers components, resolving each
 * of them through the core serde pipeline. The {@code adapter} package adapts native Kafka
 * {@code Serializer}/{@code Deserializer} beans to serde converters, {@code provider} contains
 * the record-level {@code spring-json} and {@code spring-xml} providers,
 * {@link io.github.dimkich.integration.testing.kafka.serde.KafkaHeaderSerializerConverterFactory}
 * and
 * {@link io.github.dimkich.integration.testing.kafka.serde.KafkaHeaderDeserializerConverterFactory}
 * wrap core formats into header converters, and {@code serialization}/{@code deserialization}
 * contain the concrete implementations.</p>
 */
package io.github.dimkich.integration.testing.kafka.serde;
