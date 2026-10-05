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
 * Decorator that wraps the output of a serializer into the configured binary envelope
 * (see {@link BinaryEnvelope}).
 *
 * <p>Returns the converter unchanged when no envelope is configured.
 *
 * @see BinaryEnvelopeProperties
 */
@Getter
@RequiredArgsConstructor
public class BinaryEnvelopeSerializerDecorator
        implements TestSerdeDecoratorFactory<Object, byte[], TestSerdeContext, BinaryEnvelopeProperties> {

    private final BinaryEnvelopeParser parser;

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<BinaryEnvelopeProperties> propertiesClass = BinaryEnvelopeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, byte[], TestSerdeContext> decorate(
            TestSerdeConverter<Object, byte[], TestSerdeContext> converter, BinaryEnvelopeProperties config) {
        if (!config.hasBinaryEnvelope()) {
            return converter;
        }
        BinaryEnvelope envelope = new BinaryEnvelope(parser.parse(config.getBinaryEnvelope()));
        return TestSerdeConverter.of(converter.getInputClass(), byte[].class, converter.getContextClass(),
                (input, context) -> envelope.wrap(converter.convert(input, context)));
    }
}
