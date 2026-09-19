package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * XML provider registered under the name {@code xml}; serializes values with Jackson
 * {@link XmlMapper}.
 */
@ConditionalOnClass(XmlMapper.class)
public class XmlSerdeProvider extends JacksonSerdeProvider<SerdeProperties> {

    /**
     * Creates the provider.
     *
     * @param beanResolver resolves the {@link XmlMapper} bean
     */
    public XmlSerdeProvider(BeanResolver beanResolver) {
        super(beanResolver);
    }

    @Override
    public String getName() {
        return "xml";
    }

    @Override
    protected Class<? extends ObjectMapper> getMapperClass() {
        return XmlMapper.class;
    }
}
