package io.github.dimkich.integration.testing.serde;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.date.time.MockJavaTime;
import io.github.dimkich.integration.testing.format.common.type.TypeParser;
import io.github.dimkich.integration.testing.serde.binary.BinaryEnvelopeParser;
import io.github.dimkich.integration.testing.serde.binary.BinaryLayoutTestFacade;
import io.github.dimkich.integration.testing.serde.dto.SerdeTestDto;
import io.github.dimkich.integration.testing.serde.platform.NativeBridgeDeserializer;
import io.github.dimkich.integration.testing.serde.platform.NativeBridgeSerializer;
import io.github.dimkich.integration.testing.serde.platform.adapter.NativeBridgeToCoreDeserializerAdapter;
import io.github.dimkich.integration.testing.serde.platform.adapter.NativeBridgeToCoreSerializerAdapter;
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

@MockJavaTime
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
            TaggedSerializerProvider.class,
            TaggedDeserializerProvider.class,
            TaggedRecordSerializerProvider.class,
            TaggedRecordDeserializerProvider.class,
            RecordOnlySerializerProvider.class,
            RecordOnlyDeserializerProvider.class,
            PlatformContextSerializerProvider.class,
            PlatformContextDeserializerProvider.class,
            UnrelatedContextSerializerProvider.class,
            UnrelatedContextDeserializerProvider.class,
            NativeBridgeSerializerProvider.class,
            NativeBridgeDeserializerProvider.class,
            PlainConfigStringSerializerFactory.class,
            PlainConfigBytesSerializerFactory.class,
            PlainConfigTaggedSerializerFactory.class,
            PlainConfigStringDeserializerFactory.class,
            PlainConfigBytesDeserializerFactory.class,
            PlainConfigTaggedDeserializerFactory.class,
            NativeBridgeToCoreSerializerAdapter.class,
            NativeBridgeToCoreDeserializerAdapter.class,
            MisbehavingProvider.class
    })
    static class Config {

        @Bean
        SerdeTestFacade serdeTestFacade(SerdeManager serdeManager, TypeParser typeParser, BeanResolver beanResolver,
                                        AdapterManager adapterManager) {
            return new SerdeTestFacade(serdeManager, typeParser, beanResolver, adapterManager);
        }

        @Bean
        RoleProvider roleProviderAlpha() {
            return new RoleProvider(() -> "ALPHA");
        }

        @Bean
        RoleProvider roleProviderBeta() {
            return new RoleProvider(() -> "BETA");
        }

        @Bean
        BinaryLayoutTestFacade binaryLayoutTestFacade(BinaryEnvelopeParser parser) {
            return new BinaryLayoutTestFacade(parser);
        }

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
        TaggedSerializer customSerdeSerializer() {
            return new TaggedSerializer("BEAN");
        }

        @Bean
        NativeBridgeSerializer nativeBridgeSerializer() {
            return new NativeBridgeSerializer();
        }

        @Bean
        NativeBridgeDeserializer nativeBridgeDeserializer() {
            return new NativeBridgeDeserializer();
        }
    }
}
