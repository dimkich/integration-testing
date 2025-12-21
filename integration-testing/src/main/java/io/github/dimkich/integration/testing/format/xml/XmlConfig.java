package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import io.github.dimkich.integration.testing.format.common.CommonFormatConfig;
import io.github.dimkich.integration.testing.format.common.ObjectMapperConfigurer;
import io.github.dimkich.integration.testing.format.common.scalar.ScalarTypeModule;
import io.github.dimkich.integration.testing.format.xml.attributes.BeanAsAttributesModule;
import io.github.dimkich.integration.testing.format.xml.config.jackson.Lf4SpacesIndenter;
import io.github.dimkich.integration.testing.format.xml.fixed.DefaultXmlPrettyPrinterFixed;
import io.github.dimkich.integration.testing.format.xml.fixed.XmlFactoryFixed;
import io.github.dimkich.integration.testing.format.xml.fixed.XmlMapperFixed;
import io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicUnwrappedModule;
import io.github.dimkich.integration.testing.format.xml.wrapper.WrapperModule;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(XmlMapper.class)
@Import({CommonFormatConfig.class, XmlTestTypeResolverBuilder.class})
public class XmlConfig {
    @Setter(onMethod = @__({@Autowired, @Lazy}))
    private XmlTestTypeResolverBuilder resolverBuilder;

    @Bean
    @Lazy
    XmlTestMapper xmlTestMapper(ObjectMapperConfigurer configurer) {
        XmlMapper.Builder builder = new XmlMapper.Builder(new XmlMapperFixed(new XmlFactoryFixed()));
        builder.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        builder.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        builder.enable(SerializationFeature.INDENT_OUTPUT);
        builder.enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL);
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.enable(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL);
        builder.enable(ToXmlGenerator.Feature.WRITE_XML_1_1);
        builder.defaultUseWrapper(false);

        DefaultXmlPrettyPrinter.Indenter indenter = new Lf4SpacesIndenter();
        DefaultXmlPrettyPrinter printer = new DefaultXmlPrettyPrinterFixed();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        builder.defaultPrettyPrinter(printer);

        builder.setDefaultTyping(resolverBuilder);

        configurer.configure(builder);

        builder.addModules(new SimpleModule()
                        .setDeserializerModifier(new WrapperHandlingModifier()),
                new BeanAsAttributesModule(resolverBuilder), new PolymorphicUnwrappedModule(resolverBuilder),
                new ScalarTypeModule(Set.of(Boolean.class, Integer.class, Double.class)),
                new WrapperModule());

        XmlMapper xmlMapper = builder.build();

        configurer.configure(xmlMapper);

        return new XmlTestMapper(xmlMapper);
    }
}
