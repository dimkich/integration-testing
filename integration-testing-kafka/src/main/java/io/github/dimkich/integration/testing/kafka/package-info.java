/**
 * Kafka module of integration-testing.
 *
 * <h2>Context model</h2>
 * <ul>
 *   <li>{@link io.github.dimkich.integration.testing.serde.SerdeContext} — marker interface,
 *       no methods. Prevents context-free serializers from touching transport metadata.</li>
 *   <li>{@link io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext} —
 *       platform context exposing {@code getTopic()} and native {@code Headers}.</li>
 * </ul>
 *
 * <h2>Supported configurations</h2>
 * <ol>
 *   <li>Core providers for components — {@code value.type: json}, {@code key.type: string}.</li>
 *   <li>Core providers for the whole message (payload-first) — {@code serializer.type: json}
 *       is equivalent to {@code serializer.value.type: json}. Key and headers use defaults.</li>
 *   <li>Native Kafka SerDe — any {@code org.apache.kafka.common.serialization.Serializer/Deserializer},
 *       including custom ones.</li>
 *   <li>Standard formats — {@code string}, {@code json}, {@code xml}, {@code bytes}, {@code yaml}.</li>
 *   <li>Spring Kafka native — {@code spring-json}, {@code spring-xml}; {@code __TypeId__}
 *       and header mapping handled by Spring Kafka classes natively.</li>
 * </ol>
 *
 * <h2>Headers</h2>
 * <p>Conversion between the DTO header map and native Kafka {@code Headers} happens in one
 * place — {@code KafkaRecordSerdeFactory}. Inside the framework headers flow only as native
 * {@code org.apache.kafka.common.header.Headers}.</p>
 *
 * @see io.github.dimkich.integration.testing.serde
 * @see io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext
 */
package io.github.dimkich.integration.testing.kafka;
