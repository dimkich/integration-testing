package io.github.dimkich.integration.testing.kafka.serde.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dimkich.integration.testing.TestSetupModule;
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
import org.springframework.context.annotation.Primary;
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
@Import(JsonProcessor.class)
public class JsonSerDeConfig {

    @Value("${embedded.kafka.brokerList}")
    private String brokerList;

    @Bean
    @Primary
    public ObjectMapper jsonObjectMapper() {
        return new ObjectMapper()
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
    public ConsumerFactory<String, Object> jsonConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        ObjectMapper objectMapper = jsonObjectMapper();
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(
                objectMapper.getTypeFactory().constructType(JsonTestDto.class), objectMapper) {
        };
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props, null, jsonDeserializer);
    }

    @Bean
    public ProducerFactory<String, Object> jsonProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(jsonObjectMapper());
        jsonSerializer.setAddTypeInfo(true);

        return new DefaultKafkaProducerFactory<>(props, null, jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> jsonKafkaTemplate() {
        return new KafkaTemplate<>(jsonProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, Object> springJsonKafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(jsonProducerFactory());
        template.setMessageConverter(new MessagingMessageConverter());
        return template;
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> jsonListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(byteConsumerFactory());
        factory.setRecordMessageConverter(jsonRecordMessageConverter());
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public RecordMessageConverter jsonRecordMessageConverter() {
        JsonMessageConverter converter = new JsonMessageConverter(jsonObjectMapper());
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.addTrustedPackages("*");
        converter.setTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RecordMessageConverter jsonSpringJsonMessageConverter() {
        return new MessagingMessageConverter();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> jsonSpringJsonListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(jsonConsumerFactory());
        factory.setRecordMessageConverter(jsonSpringJsonMessageConverter());
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public KafkaAdmin.NewTopics jsonTopics() {
        return new KafkaAdmin.NewTopics(
                topic("json-record-in"), topic("json-record-out"),
                topic("json-parts-in"), topic("json-parts-out"),
                topic("json-spring-json-in"), topic("json-spring-json-out"),
                topic("json-spring-json-no-type-in"), topic("json-spring-json-no-type-out")
        );
    }

    @Bean
    public TestSetupModule jsonTestSetupModule() {
        return new TestSetupModule().addSubTypes(JsonTestDto.class);
    }

    private static NewTopic topic(String name) {
        return new NewTopic(name, 1, (short) 1);
    }
}
