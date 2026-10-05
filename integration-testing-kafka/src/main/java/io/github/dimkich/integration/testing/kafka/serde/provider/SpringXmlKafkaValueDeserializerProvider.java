package io.github.dimkich.integration.testing.kafka.serde.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Component-level {@code spring-xml} deserializer: the JSON provider configured with
 * {@link XmlMapper}.
 */
@ConditionalOnClass(XmlMapper.class)
public class SpringXmlKafkaValueDeserializerProvider extends SpringJsonKafkaValueDeserializerProvider {

    @Autowired
    public SpringXmlKafkaValueDeserializerProvider(BeanResolver beanResolver,
                                                   ObjectProvider<SerdeManager> serdeManager) {
        super(beanResolver, serdeManager, "spring-xml");
    }

    @Override
    protected Class<? extends ObjectMapper> getMapperClass() {
        return XmlMapper.class;
    }
}
