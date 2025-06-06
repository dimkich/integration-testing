package io.github.dimkich.integration.testing.xml.polymorphic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import java.io.IOException;

public class PolymorphicAsPropertyTypeSerializer extends AsPropertyTypeSerializer {
    public PolymorphicAsPropertyTypeSerializer(TypeIdResolver idRes, BeanProperty property, String propName) {
        super(idRes, property, propName);
    }


    @Override
    public WritableTypeId writeTypePrefix(JsonGenerator gen, WritableTypeId idMetadata) throws IOException {
        ToXmlGenerator generator = (ToXmlGenerator) gen;
        WritableTypeId id = null;
        switch (idMetadata.valueShape) {
            case START_OBJECT -> {
                generator.setNextIsAttribute(true);
                id = super.writeTypePrefix(gen, idMetadata);
                generator.setNextIsAttribute(false);
            }
            case START_ARRAY -> {
                _generateTypeId(idMetadata);
                String currentName = generator.getOutputContext().getCurrentName();
                String idStr = (idMetadata.id instanceof String) ? (String) idMetadata.id : String.valueOf(idMetadata.id);
                idMetadata.include = WritableTypeId.Inclusion.WRAPPER_ARRAY;

                gen.writeStartObject(idMetadata.forValue);

                generator.setNextIsAttribute(true);
                gen.writeStringField(idMetadata.asProperty, idStr);
                generator.setNextIsAttribute(false);

                generator.writeFieldName(currentName == null ? "item" : currentName);
                gen.writeStartArray(idMetadata.forValue);

                id = idMetadata;
            }
        }
        return id;
    }

    @Override
    public WritableTypeId writeTypeSuffix(JsonGenerator g, WritableTypeId idMetadata) throws IOException {
        WritableTypeId typeId = super.writeTypeSuffix(g, idMetadata);
        if (typeId != null && typeId.include == WritableTypeId.Inclusion.WRAPPER_ARRAY) {
            g.writeEndObject();
        }
        return typeId;
    }

    @Override
    public AsPropertyTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this :
                new PolymorphicAsPropertyTypeSerializer(this._idResolver, prop, this._typePropertyName);
    }
}
