package io.github.dimkich.integration.testing.kafka.serde.provider;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;

/**
 * Component-level {@code spring-xml} header serializer: the JSON provider registered under
 * the {@code spring-xml} name.
 */
@ConditionalOnClass(XmlMapper.class)
public class SpringXmlKafkaHeaderSerializerProvider extends SpringJsonKafkaHeaderSerializerProvider {

    @Autowired
    public SpringXmlKafkaHeaderSerializerProvider(DefaultKafkaHeaderMapper headerMapper) {
        super(headerMapper, "spring-xml");
    }
}
