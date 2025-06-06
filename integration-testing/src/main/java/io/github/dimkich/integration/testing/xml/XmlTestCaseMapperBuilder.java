package io.github.dimkich.integration.testing.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.TestCaseMapper;
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
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
@Getter
@Setter
public class XmlTestCaseMapperBuilder {
    private final PolymorphicUnwrappedResolverBuilder typeResolverBuilder = PolymorphicUnwrappedResolverBuilder.createDefault();
    private String fileHeader = "<!-- @" + "formatter:off -->";
    private String typeFieldName = "type";
    private Class<? extends TestCase> rootTestCaseClass = TestCase.class;
    private String path;
    private List<com.fasterxml.jackson.databind.Module> modules = new ArrayList<>();
    private List<Pair<String, PropertyFilter>> filters = new ArrayList<>();

    public XmlTestCaseMapperBuilder addSubTypes(String type, Class<?> cls) {
        typeResolverBuilder.addSubType(cls, type);
        return this;
    }

    public XmlTestCaseMapperBuilder addSubTypes(Class<?>... classes) {
        typeResolverBuilder.addSubTypes(classes);
        return this;
    }

    public XmlTestCaseMapperBuilder addSubTypes(JsonSubTypes jsonSubTypes) {
        for (JsonSubTypes.Type type : jsonSubTypes.value()) {
            addSubTypes(type.name(), type.value());
        }
        return this;
    }

    public XmlTestCaseMapperBuilder addAlias(Class<?> subType, String alias) {
        typeResolverBuilder.addAlias(subType, alias);
        return this;
    }

    public XmlTestCaseMapperBuilder module(com.fasterxml.jackson.databind.Module module) {
        modules.add(module);
        return this;
    }

    public XmlTestCaseMapperBuilder filter(Pair<String, PropertyFilter> filter) {
        filters.add(filter);
        return this;
    }

    public TestCaseMapper build() {
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

        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filters.forEach(p -> filterProvider.addFilter(p.getKey(), p.getValue()));
        builder.filterProvider(filterProvider);

        XmlMapper xmlMapper = builder.build();

        SerializerFactoryConfig config = ((BasicSerializerFactory) xmlMapper.getSerializerFactory()).getFactoryConfig();
        xmlMapper.setSerializerFactory(new FixedBeanSerializerFactory(config));
        xmlMapper.setDefaultTyping(typeResolverBuilder);

        typeResolverBuilder.addParentType(TestCaseInit.class)
                .addSubTypes(EntriesObjectKeyObjectValue.class, EntriesStringKeyObjectValue.class,
                        MapStringKeyStringValue.class, MapStringKeyObjectValue.class, DateTimeInit.class,
                        MapStorageInit.class, TestDataStorageInit.class, BeanInit.class,
                        TablesStorageSetup.class, TablesStorageInit.class, ResponseEntity.class, SpringErrorDto.class,
                        HttpMethod.class);

        SimpleModule module = new SimpleModule();
        ObjectToLocationStorage objectToLocationStorage = new ObjectToLocationStorage();
        module.setDeserializerModifier(new StoreLocationBeanDeserializerModifier(objectToLocationStorage));
        module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        module.setMixInAnnotation(Throwable.class, ThrowableMixIn.class);
        module.setMixInAnnotation(ResponseEntity.class, ResponseEntityMixIn.class);
        module.setMixInAnnotation(HttpStatusCode.class, ResponseEntityMixIn.HttpStatusMixIn.class);
        module.setMixInAnnotation(RestClientResponseException.class, ResponseEntityMixIn.RestClientResponseExceptionMixin.class);
        module.setMixInAnnotation(FieldError.class, FieldErrorMixIn.class);
        module.setMixInAnnotation(HttpMethod.class, HttpMethodMixIn.class);
        xmlMapper.registerModule(module);

        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.registerModule(new BeanAsAttributesModule(typeResolverBuilder.getTypeAttributes()));
        xmlMapper.registerModule(new PolymorphicUnwrappedModule(typeResolverBuilder));
        xmlMapper.registerModule(new MapModule());

        modules.forEach(xmlMapper::registerModule);

        return new XmlTestCaseMapper(xmlMapper, path, fileHeader, objectToLocationStorage, rootTestCaseClass);
    }
}
