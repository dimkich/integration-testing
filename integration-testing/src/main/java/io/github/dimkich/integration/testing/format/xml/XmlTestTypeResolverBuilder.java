package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.common.TestTypeResolverBuilder;
import io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicAsPropertyTypeDeserializer;
import io.github.dimkich.integration.testing.format.xml.polymorphic.PolymorphicAsPropertyTypeSerializer;

import java.util.Collection;
import java.util.List;

public class XmlTestTypeResolverBuilder extends TestTypeResolverBuilder {
    public XmlTestTypeResolverBuilder(List<TestSetupModule> modules) {
        super(modules);
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        if (parentToSubTypeMap.containsKey(baseType.getRawClass())) {
            TypeSerializer typeSerializer = super.buildTypeSerializer(config, baseType, parentToSubTypeMap.get(baseType.getRawClass()));
            return new PolymorphicAsPropertyTypeSerializer(typeSerializer.getTypeIdResolver(), null,
                    typeSerializer.getPropertyName());
        }
        return null;
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        if (parentToSubTypeMap.containsKey(baseType.getRawClass())) {
            TypeDeserializer typeDeserializer = super.buildTypeDeserializer(config, baseType, parentToSubTypeMap.get(baseType.getRawClass()));
            return new PolymorphicAsPropertyTypeDeserializer(this, (AsPropertyTypeDeserializer) typeDeserializer, null);
        }
        return null;
    }
}
