package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.deserialization.CoreKafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;

/**
 * Wraps a core {@link TestSerdeDeserializer} into a
 * {@link KafkaHeaderDeserializer} using {@link CoreKafkaHeaderDeserializer}.
 * <p>
 * The wildcard {@code ? extends SerdeContext} accepts any provider that is
 * compatible with a bare {@code SerdeContext}: only context-free providers
 * (declaring {@code SerdeContext}) are allowed, validated
 * by {@link TestSerdeDeserializer#assertAcceptsContext} at adapt time. Context-aware providers
 * declaring a {@code SerdeContext} subtype are rejected up front instead of
 * failing later with {@code ClassCastException}, because headers carry no
 * transport context.
 */
public class TestSerdeDeserializerToKafkaHeaderDeserializerAdapter
        implements TestSerdeAdapter<TestSerdeDeserializer<Object, ?>, KafkaHeaderDeserializer, SerdeProperties> {

    @Override
    public KafkaHeaderDeserializer adapt(TestSerdeDeserializer<Object, ?> source, SerdeProperties properties) {
        source.assertAcceptsContext(SerdeContext.class,
                SerdeAdapterMessages.CONTEXT_FREE_HEADER_HINT);
        return new CoreKafkaHeaderDeserializer(source);
    }
}
