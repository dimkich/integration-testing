package io.github.dimkich.integration.testing.format.xml.attributes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import eu.ciechanowiec.sneakyfun.SneakyFunction;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.util.Set;

public class BeanAsAttributesDeserializer extends DelegatingDeserializer {
    private final SettableBeanProperty property;
    private final Set<String> typeAttributes;

    public BeanAsAttributesDeserializer(JsonDeserializer<?> d, SettableBeanProperty property, Set<String> typeAttributes) {
        super(d);
        this.property = property;
        this.typeAttributes = typeAttributes;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return deserialize(p, ctxt, (jp) -> super.deserialize(jp, ctxt));
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object bean) throws IOException {
        return deserialize(p, ctxt, (jp) -> super.deserialize(jp, ctxt, bean));
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return deserialize(p, ctxt, (jp) -> super.deserializeWithType(jp, ctxt, typeDeserializer));
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer, Object intoValue) throws IOException {
        return deserialize(p, ctxt, (jp) -> super.deserializeWithType(jp, ctxt, typeDeserializer, intoValue));
    }

    private Object deserialize(JsonParser p, DeserializationContext ctxt,
                               SneakyFunction<JsonParser, Object, IOException> function) throws IOException {
        XMLStreamReader xmlStreamReader = ((FromXmlParser) p).getStaxReader();

        String name;
        TokenBuffer tokenBuffer = null;
        TokenBuffer anySetterBuffer = null;
        for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
            p.nextToken();
            name = p.currentName();
            p.nextToken();
            if (typeAttributes.contains(name)) {
                if (tokenBuffer == null) {
                    tokenBuffer = ctxt.bufferForInputBuffering();
                }
                tokenBuffer.writeFieldName(name);
                tokenBuffer.writeString(p.getValueAsString());
            } else {
                if (anySetterBuffer == null) {
                    anySetterBuffer = ctxt.bufferForInputBuffering();
                }
                anySetterBuffer.writeFieldName(name);
                anySetterBuffer.writeString(p.getValueAsString());
            }
        }
        Object value = null;
        if (anySetterBuffer != null) {
            JsonDeserializer<Object> ser = ctxt.findNonContextualValueDeserializer(property.getType());
            JsonParser anyParser = anySetterBuffer.asParser();
            anyParser.nextToken();
            value = ser.deserialize(anyParser, ctxt);
        }

        if (tokenBuffer != null) {
            p = JsonParserSequence.createFlattened(false, tokenBuffer.asParser(p), p);
        }
        p.nextToken();
        Object bean = function.apply(p);
        property.set(bean, value);
        return bean;
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new BeanAsAttributesDeserializer(newDelegatee, property, typeAttributes);
    }
}
