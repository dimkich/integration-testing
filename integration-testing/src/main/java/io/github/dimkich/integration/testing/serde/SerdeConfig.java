package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.serde.binary.*;
import io.github.dimkich.integration.testing.serde.properties.*;
import io.github.dimkich.integration.testing.serde.providers.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring configuration of the serde subsystem: registers the managers, built-in converter
 * factories, providers and binary envelope segment providers. Imported by the integration test
 * configuration.
 */
@Configuration
@Import({
        BeanResolver.class,
        TypeConverter.class,
        SerdeManager.class,
        ConverterManager.class,
        DecoratorManager.class,
        AdapterManager.class,
        ProviderManager.class,
        BeanRefConverterFactory.class,
        ClassRefConverterFactory.class,
        ProviderConverterFactory.class,
        BinaryEnvelopeSerializerDecorator.class,
        BinaryEnvelopeDeserializerDecorator.class,
        StringSerializerFactory.class,
        StringDeserializerFactory.class,
        ByteArraySerializerFactory.class,
        ByteArrayDeserializerFactory.class,
        JsonSerializerFactory.class,
        JsonDeserializerFactory.class,
        XmlSerializerFactory.class,
        XmlDeserializerFactory.class,
        YamlSerializerFactory.class,
        YamlDeserializerFactory.class,
        BinaryEnvelopeParser.class,
        ContentSegmentProvider.class,
        LengthSegmentProvider.class,
        TsSegmentProvider.class,
        FixSegmentProvider.class,
        Crc32SegmentProvider.class,
        VersionSegmentProvider.class,
        StringSegmentProvider.class
})
public class SerdeConfig {
}
