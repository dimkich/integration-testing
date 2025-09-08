package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.WrapperHandlingDeserializer;
import com.fasterxml.jackson.dataformat.xml.deser.XmlBeanDeserializerModifier;

public class WrapperHandlingModifier extends XmlBeanDeserializerModifier {
    public WrapperHandlingModifier() {
        super(FromXmlParser.DEFAULT_UNNAMED_TEXT_PROPERTY);
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        JsonDeserializer<?> deser = super.modifyDeserializer(config, beanDesc, deserializer);
        if (deser instanceof WrapperHandlingDeserializer wrapper) {
            deser = new WrapperHandlingDeserializerFixed((BeanDeserializerBase) wrapper.getDelegatee());
        }
        return deser;
    }
}
