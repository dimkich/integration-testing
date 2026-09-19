package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.serialization.CoreKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaHeaderSerializer;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;

/**
 * Wraps a core {@link TestSerdeSerializer} into a
 * {@link KafkaHeaderSerializer} using {@link CoreKafkaHeaderSerializer}.
 * <p>
 * The wildcard {@code ? extends SerdeContext} accepts any provider that is
 * compatible with a bare {@code SerdeContext}: only context-free providers
 * (declaring {@code SerdeContext}) are allowed, validated
 * by {@link TestSerdeSerializer#assertAcceptsContext} at adapt time. Context-aware providers
 * declaring a {@code SerdeContext} subtype are rejected up front instead of
 * failing later with {@code ClassCastException}, because headers carry no
 * transport context.
 */
public class TestSerdeSerializerToKafkaHeaderSerializerAdapter
        implements TestSerdeAdapter<TestSerdeSerializer<Object, ?>, KafkaHeaderSerializer, SerdeProperties> {

    @Override
    public KafkaHeaderSerializer adapt(TestSerdeSerializer<Object, ?> source, SerdeProperties properties) {
        source.assertAcceptsContext(SerdeContext.class,
                SerdeAdapterMessages.CONTEXT_FREE_HEADER_HINT);
        return new CoreKafkaHeaderSerializer(source);
    }
}
