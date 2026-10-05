package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeDecoratorFactory;
import io.github.dimkich.integration.testing.serde.binary.BinaryEnvelope;
import io.github.dimkich.integration.testing.serde.binary.BinaryEnvelopeParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * Decorator that unwraps the configured binary envelope (see {@link BinaryEnvelope}) before the
 * deserializer is applied.
 *
 * <p>Returns the converter unchanged when no envelope is configured.
 *
 * @see BinaryEnvelopeProperties
 */
@Getter
@RequiredArgsConstructor
public class BinaryEnvelopeDeserializerDecorator
        implements TestSerdeDecoratorFactory<byte[], Object, TestSerdeContext, BinaryEnvelopeProperties> {

    private final BinaryEnvelopeParser parser;

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<BinaryEnvelopeProperties> propertiesClass = BinaryEnvelopeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<byte[], Object, TestSerdeContext> decorate(
            TestSerdeConverter<byte[], Object, TestSerdeContext> converter, BinaryEnvelopeProperties config) {
        if (!config.hasBinaryEnvelope()) {
            return converter;
        }
        BinaryEnvelope envelope = new BinaryEnvelope(parser.parse(config.getBinaryEnvelope()));
        return TestSerdeConverter.of(byte[].class, converter.getOutputClass(), converter.getContextClass(),
                (input, context) -> {
                    byte[] payload = envelope.unwrap(input);
                    return payload == null ? null : converter.convert(payload, context);
                });
    }
}
