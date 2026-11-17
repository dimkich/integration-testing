package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.github.dimkich.integration.testing.config.SimpleTypeDetector;
import io.github.dimkich.integration.testing.util.FastPath;

public class PolymorphicUnwrappedBeanPropertyWriter extends UnwrappingBeanPropertyWriter {
    private final TypeSerializer unwrappedTypeSerializer;
    private final String unwrappedTypeProperty;
    private final SimpleTypeDetector simpleTypeDetector;

    public PolymorphicUnwrappedBeanPropertyWriter(BeanPropertyWriter base, NameTransformer nameTransformer,
                                                  TypeSerializer unwrappedTypeSerializer, String unwrappedTypeProperty,
                                                  SimpleTypeDetector simpleTypeDetector) {
        super(base, nameTransformer);
        this.unwrappedTypeSerializer = unwrappedTypeSerializer;
        this.unwrappedTypeProperty = unwrappedTypeProperty;
        this.simpleTypeDetector = simpleTypeDetector;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        ToXmlGenerator generator = (ToXmlGenerator) gen;
        Object value = get(bean);
        // StringSerializer.serializeWithType ignores the type serializer and writes the string as a
        // natural scalar. Mirror that decision here so unwrapped strings do not get a type attribute.
        if (value != null && unwrappedTypeSerializer != null
                && !(prov.findValueSerializer(value.getClass()) instanceof StringSerializer)) {
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
        return FastPath.isStructuredValue(value.getClass(), simpleTypeDetector, prov);
    }
}
