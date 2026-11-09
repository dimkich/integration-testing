package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class PolymorphicUnwrappedBeanPropertyWriter extends UnwrappingBeanPropertyWriter {
    private final TypeSerializer unwrappedTypeSerializer;
    private final String unwrappedTypeProperty;

    public PolymorphicUnwrappedBeanPropertyWriter(BeanPropertyWriter base, NameTransformer nameTransformer,
                                                  TypeSerializer unwrappedTypeSerializer, String unwrappedTypeProperty) {
        super(base, nameTransformer);
        this.unwrappedTypeSerializer = unwrappedTypeSerializer;
        this.unwrappedTypeProperty = unwrappedTypeProperty;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        ToXmlGenerator generator = (ToXmlGenerator) gen;
        Object value = get(bean);
        if (value != null && unwrappedTypeSerializer != null) {
            generator.setNextIsAttribute(true);
            generator.writeStringField(unwrappedTypeProperty, unwrappedTypeSerializer.getTypeIdResolver().idFromValue(value));
            generator.setNextIsAttribute(false);
        }

        generator.setNextIsUnwrapped(!isBean(bean, prov));
        super.serializeAsField(bean, gen, prov);
        generator.setNextIsUnwrapped(false);
    }

    private boolean isBean(Object bean, SerializerProvider prov) throws Exception {
        Object value = get(bean);
        if (value == null) {
            return true;
        }
        Class<?> cls = value.getClass();
        if (cls.isPrimitive() || cls.isArray() || Number.class.isAssignableFrom(cls)) {
            return false;
        }
        if (Map.class.isAssignableFrom(cls) || Collection.class.isAssignableFrom(cls)
                || Iterable.class.isAssignableFrom(cls) || Iterator.class.isAssignableFrom(cls)) {
            return true;
        }
        return isBeanSerializer(prov.findValueSerializer(cls));
    }

    private boolean isBeanSerializer(JsonSerializer<?> serializer) {
        while (serializer != null) {
            if (serializer instanceof BeanSerializerBase) {
                return true;
            }
            serializer = serializer.getDelegatee();
        }
        return false;
    }
}
