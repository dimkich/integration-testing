package io.github.dimkich.integration.testing.serde;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.serde.dto.SerdeTestDto;
import io.github.dimkich.integration.testing.serde.platform.adapter.*;
import io.github.dimkich.integration.testing.serde.provider.*;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.stream.Stream;

@IntegrationTesting
@SpringBootTest(classes = SerdeTest.Config.class)
public class SerdeTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("serde/serde.xml");
    }

    @Configuration
    @Import({
            SerdeTestFacade.class,
            TaggedSerdeProvider.class,
            TaggedRecordSerdeProvider.class,
            RecordOnlySerdeProvider.class,
            PlatformContextSerdeProvider.class,
            UnrelatedContextSerdeProvider.class,
            DirectPlatformSerdeProvider.class,
            CoreToPlatformSerializerAdapter.class,
            CoreToPlatformDeserializerAdapter.class,
            SpecificConfigCoreToPlatformSerializerAdapter.class,
            SpecificConfigCoreToPlatformDeserializerAdapter.class,
            PlatformSerializerToPlatformRecordSerializerAdapter.class,
            PlatformDeserializerToPlatformRecordDeserializerAdapter.class,
            TestSerdeSerializerToPlatformRecordSerializerAdapter.class,
            TestSerdeDeserializerToPlatformRecordDeserializerAdapter.class,
            AmbiguousAlphaAdapter.class,
            AmbiguousBetaAdapter.class
    })
    static class Config {

        @Bean
        @Primary
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ObjectMapper altObjectMapper() {
            return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        @Bean
        XmlMapper xmlMapper() {
            return new XmlMapper();
        }

        @Bean
        YAMLMapper yamlMapper() {
            return new YAMLMapper();
        }

        @Bean
        TestSetupModule serdeTestModule() {
            return new TestSetupModule().addSubTypes(SerdeTestDto.class);
        }

        @Bean
        TestSerdeSerializer<Object, SerdeContext> customSerdeSerializer() {
            return new TaggedSerializer("BEAN");
        }
    }
}
