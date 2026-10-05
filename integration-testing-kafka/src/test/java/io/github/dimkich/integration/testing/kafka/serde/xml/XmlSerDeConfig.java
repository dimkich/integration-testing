package io.github.dimkich.integration.testing.kafka.serde.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dimkich.integration.testing.kafka.serde.json.JsonTestDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Import(XmlProcessor.class)
public class XmlSerDeConfig {

    @Value("${embedded.kafka.brokerList}")
    private String brokerList;

    @Bean
    public XmlMapper xmlObjectMapper() {
        return (XmlMapper) new XmlMapper()
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public ConsumerFactory<String, Object> byteConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, Object> xmlConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        ObjectMapper objectMapper = xmlObjectMapper();
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(
                objectMapper.getTypeFactory().constructType(JsonTestDto.class), objectMapper) {
        };
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props, null, jsonDeserializer);
    }

    @Bean
    public ProducerFactory<String, Object> xmlProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(xmlObjectMapper());
        jsonSerializer.setAddTypeInfo(true);

        return new DefaultKafkaProducerFactory<>(props, null, jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> xmlKafkaTemplate() {
        return new KafkaTemplate<>(xmlProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, Object> springXmlKafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(xmlProducerFactory());
        template.setMessageConverter(new MessagingMessageConverter());
        return template;
    }

    @Bean
    public ProducerFactory<String, Object> springXmlNoTypeProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(xmlObjectMapper());
        jsonSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(props, null, jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> springXmlNoTypeKafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(springXmlNoTypeProducerFactory());
        template.setMessageConverter(new MessagingMessageConverter());
        return template;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> xmlListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(byteConsumerFactory());
        factory.setRecordMessageConverter(xmlRecordMessageConverter());
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public RecordMessageConverter xmlRecordMessageConverter() {
        JsonMessageConverter converter = new JsonMessageConverter(xmlObjectMapper());
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.addTrustedPackages("*");
        converter.setTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RecordMessageConverter xmlSpringXmlMessageConverter() {
        return new MessagingMessageConverter();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> xmlSpringXmlListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(xmlConsumerFactory());
        factory.setRecordMessageConverter(xmlSpringXmlMessageConverter());
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public KafkaAdmin.NewTopics xmlTopics() {
        return new KafkaAdmin.NewTopics(
                topic("xml-spring-xml-in"), topic("xml-spring-xml-out"),
                topic("xml-spring-xml-no-type-in"), topic("xml-spring-xml-no-type-out"),
                topic("xml-record-in"), topic("xml-record-out")
        );
    }

    private static NewTopic topic(String name) {
        return new NewTopic(name, 1, (short) 1);
    }
}
