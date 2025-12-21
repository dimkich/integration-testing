package io.github.dimkich.integration.testing.format.common.type;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

@Component("testTypeResolverBuilder")
@RequiredArgsConstructor
public class TestTypeResolverBuilder extends StdTypeResolverBuilder {
    private final TypeResolverFactory typeResolverFactory;

    @Getter
    private String unwrappedTypeProperty;

    @PostConstruct
    void init() {
        init(JsonTypeInfo.Id.NAME, null);
        inclusion(JsonTypeInfo.As.PROPERTY);
        typeProperty("type");
        unwrappedTypeProperty = "utype";
    }

    public Set<String> getTypeAttributes() {
        return Set.of(getTypeProperty(), getUnwrappedTypeProperty());
    }

    public boolean isCollection(String type) {
        return typeResolverFactory.isCollection(type);
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        TypeIdResolver idResolver = typeResolverFactory.createTypeIdResolver(config, baseType);
        if (idResolver == null) {
            return null;
        }
        return new AsPropertyTypeSerializer(idResolver, null, _typeProperty);
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        TypeIdResolver idResolver = typeResolverFactory.createTypeIdResolver(config, baseType);
        if (idResolver == null) {
            return null;
        }
        return new AsPropertyTypeDeserializer(baseType, idResolver,
                _typeProperty, _typeIdVisible, null, _includeAs,
                _strictTypeIdHandling(config, baseType));
    }
}
