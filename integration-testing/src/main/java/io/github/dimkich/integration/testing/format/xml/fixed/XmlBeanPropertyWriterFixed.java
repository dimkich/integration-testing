package io.github.dimkich.integration.testing.format.xml.fixed;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.dataformat.xml.ser.XmlBeanPropertyWriter;

public class XmlBeanPropertyWriterFixed extends XmlBeanPropertyWriter {
    public XmlBeanPropertyWriterFixed(BeanPropertyWriter wrapped, PropertyName wrapperName,
                                      PropertyName wrappedName) {
        super(wrapped, wrapperName, wrappedName);
    }

    public XmlBeanPropertyWriterFixed(BeanPropertyWriter wrapped, PropertyName wrapperName,
                                      PropertyName wrappedName, JsonSerializer<Object> serializer) {
        super(wrapped, wrapperName, wrappedName, serializer);
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception {
        Object value = get(bean);
        if (value == null) {
            if ((_suppressableValue != null)
                    && prov.includeFilterSuppressNulls(_suppressableValue)) {
                return;
            }
            if (_nullSerializer != null) {
                jgen.writeFieldName(_name);
                _nullSerializer.serialize(null, jgen, prov);
            }
            return;
        }
        super.serializeAsField(bean, jgen, prov);
    }
}
