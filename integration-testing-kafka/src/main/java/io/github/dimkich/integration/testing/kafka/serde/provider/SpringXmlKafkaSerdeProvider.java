package io.github.dimkich.integration.testing.kafka.serde.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Record-level {@code spring-xml} provider.
 *
 * <p>Available only at record level. Uses Spring Kafka's XML serialization via
 * {@link com.fasterxml.jackson.dataformat.xml.XmlMapper}, which writes type-info
 * into {@code Headers}. This is incompatible with the context-free component
 * contract, so the provider is not registered with
 * {@link io.github.dimkich.integration.testing.serde.SerdeProperties} and
 * cannot be used as {@code value}/{@code key}/{@code headers}.type.
 *
 * <p>To combine type-info serialization with custom key or headers, use this
 * provider at record level and override {@code key}/{@code headers} nested:
 * <pre>{@code
 * serializer:
 *   type: spring-xml
 *   add-type-info-headers: true
 *   key:
 *     type: io.github.dimkich.integration.testing.serde.impl.StringSerializer
 *   headers:
 *     type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
 * }</pre>
 */
@ConditionalOnClass(XmlMapper.class)
public class SpringXmlKafkaSerdeProvider extends SpringJsonKafkaSerdeProvider {

    /**
     * Creates the provider.
     *
     * @param beanResolver resolves the {@link XmlMapper} bean
     */
    public SpringXmlKafkaSerdeProvider(BeanResolver beanResolver) {
        super(beanResolver);
    }

    @Override
    public String getName() {
        return "spring-xml";
    }

    @Override
    protected Class<? extends ObjectMapper> getMapperClass() {
        return XmlMapper.class;
    }
}
