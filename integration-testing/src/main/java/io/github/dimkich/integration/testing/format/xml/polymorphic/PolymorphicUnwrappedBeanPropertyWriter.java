package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

public class PolymorphicUnwrappedBeanPropertyWriter extends UnwrappingBeanPropertyWriter {
    private final TypeSerializer unwrappedTypeSerializer;
    private final String unwrappedTypeProperty;

    public PolymorphicUnwrappedBeanPropertyWriter(BeanPropertyWriter base, NameTransformer unwrapper,
                                                  TypeSerializer unwrappedTypeSerializer, String unwrappedTypeProperty) {
        super(base, unwrapper);
        this.unwrappedTypeSerializer = unwrappedTypeSerializer;
        this.unwrappedTypeProperty = unwrappedTypeProperty;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        ToXmlGenerator toXmlGenerator = (ToXmlGenerator) gen;

        ToXmlGenerator generator = (ToXmlGenerator) gen;
        Object value = get(bean);
        if (value != null && unwrappedTypeSerializer != null) {
            generator.setNextIsAttribute(true);
            generator.writeStringField(unwrappedTypeProperty, unwrappedTypeSerializer.getTypeIdResolver().idFromValue(value));
            generator.setNextIsAttribute(false);
        }

        toXmlGenerator.setNextIsUnwrapped(!isBean(bean));
        super.serializeAsField(bean, gen, prov);
        toXmlGenerator.setNextIsUnwrapped(false);
    }

    private boolean isBean(Object bean) throws Exception {
        Object value = get(bean);
        if (value == null) {
            return true;
        }
        Class<?> cls = value.getClass();
        if (cls.isPrimitive() || Number.class.isAssignableFrom(cls)) {
            return false;
        }
        if (cls.getPackage().getName().startsWith("java.util")) {
            return true;
        }
        return !cls.getPackage().getName().startsWith("java");
    }
}
