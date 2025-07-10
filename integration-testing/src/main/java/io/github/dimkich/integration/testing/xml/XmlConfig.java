package io.github.dimkich.integration.testing.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dimkich.integration.testing.Module;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.initialization.*;
import io.github.dimkich.integration.testing.openapi.FieldErrorMixIn;
import io.github.dimkich.integration.testing.openapi.SpringErrorDto;
import io.github.dimkich.integration.testing.storage.mapping.EntriesObjectKeyObjectValue;
import io.github.dimkich.integration.testing.storage.mapping.EntriesStringKeyObjectValue;
import io.github.dimkich.integration.testing.storage.mapping.MapStringKeyObjectValue;
import io.github.dimkich.integration.testing.storage.mapping.MapStringKeyStringValue;
import io.github.dimkich.integration.testing.xml.attributes.BeanAsAttributesModule;
import io.github.dimkich.integration.testing.xml.config.jackson.BigDecimalSerializer;
import io.github.dimkich.integration.testing.xml.config.jackson.Lf4SpacesIndenter;
import io.github.dimkich.integration.testing.xml.config.jackson.ThrowableMixIn;
import io.github.dimkich.integration.testing.xml.map.MapModule;
import io.github.dimkich.integration.testing.xml.polymorphic.PolymorphicUnwrappedModule;
import io.github.dimkich.integration.testing.xml.polymorphic.PolymorphicUnwrappedResolverBuilder;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.validation.FieldError;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Import({ObjectToLocationStorage.class, PolymorphicUnwrappedResolverBuilder.class})
public class XmlConfig {
    private final ObjectToLocationStorage objectToLocationStorage;
    @Setter(onMethod = @__({@Autowired, @Lazy}))
    private PolymorphicUnwrappedResolverBuilder resolverBuilder;

    @Bean
    Module xmlModule() {
        SimpleModule jacksonModule = new SimpleModule();
        jacksonModule.setDeserializerModifier(new StoreLocationBeanDeserializerModifier(objectToLocationStorage));
        jacksonModule.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        jacksonModule.setMixInAnnotation(Throwable.class, ThrowableMixIn.class);
        jacksonModule.setMixInAnnotation(FieldError.class, FieldErrorMixIn.class);

        return new Module()
                .addParentType(TestCaseInit.class)
                .addSubTypes(EntriesObjectKeyObjectValue.class, EntriesStringKeyObjectValue.class,
                        MapStringKeyStringValue.class, MapStringKeyObjectValue.class, DateTimeInit.class,
                        KeyValueStorageInit.class, BeanInit.class, MockInit.class,
                        SqlStorageSetup.class, SqlStorageInit.class, SpringErrorDto.class)
                .addJacksonModule(jacksonModule)
                .addJacksonModule(new JavaTimeModule())
                .addJacksonModule(new BeanAsAttributesModule(resolverBuilder))
                .addJacksonModule(new PolymorphicUnwrappedModule(resolverBuilder))
                .addJacksonModule(new MapModule());
    }

    @Bean
    XmlTestCaseMapper testCaseMapper(List<Module> modules) {
        XmlMapper.Builder builder = XmlMapper.builder();
        builder.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        builder.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        builder.enable(SerializationFeature.INDENT_OUTPUT);
        builder.enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL);
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.enable(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL);
        builder.enable(ToXmlGenerator.Feature.WRITE_XML_1_1);
        builder.defaultUseWrapper(false);

        DefaultXmlPrettyPrinter.Indenter indenter = new Lf4SpacesIndenter();
        DefaultXmlPrettyPrinter printer = new DefaultXmlPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        builder.defaultPrettyPrinter(printer);

        builder.setDefaultTyping(resolverBuilder);

        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        for (Module module : modules) {
            module.getJacksonModules().forEach(builder::addModules);
            module.getJacksonFilters().forEach(f -> filterProvider.addFilter(f.getKey(), f.getValue()));
            if (module.getHandlerInstantiator() != null) {
                builder.handlerInstantiator(module.getHandlerInstantiator());
            }
        }
        builder.filterProvider(filterProvider);

        XmlMapper xmlMapper = builder.build();

        SerializerFactoryConfig config = ((BasicSerializerFactory) xmlMapper.getSerializerFactory()).getFactoryConfig();
        xmlMapper.setSerializerFactory(new FixedBeanSerializerFactory(config));

        return new XmlTestCaseMapper(xmlMapper, objectToLocationStorage);
    }
}
