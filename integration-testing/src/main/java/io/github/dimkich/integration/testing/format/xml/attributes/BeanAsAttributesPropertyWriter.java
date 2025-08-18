package io.github.dimkich.integration.testing.format.xml.attributes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.AnyGetterWriter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

public class BeanAsAttributesPropertyWriter extends UnwrappingBeanPropertyWriter {
    private final AnyGetterFinder anyGetterFinder;

    public BeanAsAttributesPropertyWriter(BeanPropertyWriter base, NameTransformer unwrapper,
                                          AnyGetterFinder anyGetterFinder) {
        super(base, unwrapper);
        this.anyGetterFinder = anyGetterFinder;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        ToXmlGenerator generator = (ToXmlGenerator) gen;
        Object value = get(bean);
        if (value != null) {
            AnyGetterWriter anyGetterWriter = anyGetterFinder.find(value.getClass(), prov);
            generator.setNextIsAttribute(true);
            anyGetterWriter.getAndSerialize(value, gen, prov);
            generator.setNextIsAttribute(false);
        }
    }
}
