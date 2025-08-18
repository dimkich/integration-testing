package io.github.dimkich.integration.testing.format.xml;

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
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.xml.attributes.BeanAsAttributesModule;
import io.github.dimkich.integration.testing.format.xml.config.jackson.Lf4SpacesIndenter;
import io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicUnwrappedModule;
import io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicUnwrappedResolverBuilder;
import io.github.dimkich.integration.testing.format.xml.map.MapModule;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(XmlMapper.class)
@Import({ObjectToLocationStorage.class, PolymorphicUnwrappedResolverBuilder.class})
public class XmlConfig {
    private final ObjectToLocationStorage objectToLocationStorage;
    @Setter(onMethod = @__({@Autowired, @Lazy}))
    private PolymorphicUnwrappedResolverBuilder resolverBuilder;

    @Bean
    TestSetupModule xmlModule() {
        SimpleModule jacksonModule = new SimpleModule();
        jacksonModule.setDeserializerModifier(new StoreLocationBeanDeserializerModifier(objectToLocationStorage));

        return new TestSetupModule()
                .addJacksonModule(jacksonModule)
                .addJacksonModule(new BeanAsAttributesModule(resolverBuilder))
                .addJacksonModule(new PolymorphicUnwrappedModule(resolverBuilder))
                .addJacksonModule(new MapModule());
    }

    @Bean
    XmlTestMapper testMapper(List<TestSetupModule> modules) {
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
        for (TestSetupModule module : modules) {
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

        return new XmlTestMapper(xmlMapper, objectToLocationStorage);
    }
}
