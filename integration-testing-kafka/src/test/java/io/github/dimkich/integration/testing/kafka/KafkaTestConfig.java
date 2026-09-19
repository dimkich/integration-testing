package io.github.dimkich.integration.testing.kafka;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped.DisablePolymorphicUnwrappedModule;
import io.github.dimkich.integration.testing.kafka.serde.ReceivedMessageStorage;
import io.github.dimkich.integration.testing.kafka.serde.bytes.ByteSerDeConfig;
import io.github.dimkich.integration.testing.kafka.serde.json.JsonSerDeConfig;
import io.github.dimkich.integration.testing.kafka.serde.misc.MiscSerDeConfig;
import io.github.dimkich.integration.testing.kafka.serde.text.TextSerDeConfig;
import io.github.dimkich.integration.testing.kafka.serde.xml.XmlSerDeConfig;
import io.github.dimkich.integration.testing.kafka.uncommitted.UncommittedOffsetConfig;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.KafkaNull;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@Import({ReceivedMessageStorage.class, TextSerDeConfig.class, JsonSerDeConfig.class, ByteSerDeConfig.class, MiscSerDeConfig.class, XmlSerDeConfig.class, UncommittedOffsetConfig.class})
public class KafkaTestConfig {

    @Value("${embedded.kafka.brokerList}")
    private String brokerList;

    @Bean
    public TestSetupModule testModule() {
        return new TestSetupModule()
                .addSubTypes(OrderDto.class, OrderEventDto.class, KafkaNull.class,
                        MismatchedInputException.class, InvalidDefinitionException.class)
                .addJacksonModule(new DisablePolymorphicUnwrappedModule());
    }

    @Bean
    public TestBean testBean() {
        return new TestBean();
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        return new KafkaAdmin(props);
    }
}
