package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Provider {@code xml}: Jackson XML deserializer backed by {@link XmlMapper}, registered when
 * Jackson XML is on the classpath.
 */
@ConditionalOnClass(XmlMapper.class)
public class XmlDeserializerFactory extends JacksonDeserializerFactory {

    /**
     * Creates the factory.
     *
     * @param beanResolver resolves the mapper bean of the provider
     */
    public XmlDeserializerFactory(BeanResolver beanResolver) {
        super(beanResolver, "xml");
    }

    @Override
    protected Class<? extends ObjectMapper> getMapperClass() {
        return XmlMapper.class;
    }
}
