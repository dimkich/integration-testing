package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import io.github.dimkich.integration.testing.format.common.type.TestTypeResolverBuilder;
import io.github.dimkich.integration.testing.format.common.type.TypeResolverFactory;
import io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicAsPropertyTypeDeserializer;
import io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicAsPropertyTypeSerializer;

import java.util.Collection;

public class XmlTestTypeResolverBuilder extends TestTypeResolverBuilder {
    public XmlTestTypeResolverBuilder(TypeResolverFactory factory) {
        super(factory);
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        TypeSerializer typeSerializer = super.buildTypeSerializer(config, baseType, null);
        if (typeSerializer != null) {
            return new PolymorphicAsPropertyTypeSerializer(typeSerializer.getTypeIdResolver(), null,
                    typeSerializer.getPropertyName());
        }
        return null;
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        TypeDeserializer typeDeserializer = super.buildTypeDeserializer(config, baseType, null);
        if (typeDeserializer != null) {
            return new PolymorphicAsPropertyTypeDeserializer(this, (AsPropertyTypeDeserializer) typeDeserializer, null);
        }
        return null;
    }
}
