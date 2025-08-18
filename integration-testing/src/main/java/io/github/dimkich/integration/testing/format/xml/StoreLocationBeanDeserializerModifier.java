package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.WrapperHandlingDeserializer;
import com.fasterxml.jackson.dataformat.xml.deser.XmlBeanDeserializerModifier;

public class StoreLocationBeanDeserializerModifier extends XmlBeanDeserializerModifier {
    private final ObjectToLocationStorage objectToLocationStorage;

    public StoreLocationBeanDeserializerModifier(ObjectToLocationStorage objectToLocationStorage) {
        super(FromXmlParser.DEFAULT_UNNAMED_TEXT_PROPERTY);
        this.objectToLocationStorage = objectToLocationStorage;
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        JsonDeserializer<?> deser = super.modifyDeserializer(config, beanDesc, deserializer);
        if (deser instanceof WrapperHandlingDeserializer wrapper) {
            deser = new WrapperHandlingDeserializerFixed((BeanDeserializerBase)wrapper.getDelegatee());
        }
        return new StoreLocationSerializer(deser, objectToLocationStorage);
    }
}
