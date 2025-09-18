package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import io.github.dimkich.integration.testing.format.common.TestTypeResolverBuilder;
import io.github.dimkich.integration.testing.format.util.JacksonUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;

public class PolymorphicAsPropertyTypeDeserializer extends AsPropertyTypeDeserializer {
    private final TestTypeResolverBuilder builder;

    public PolymorphicAsPropertyTypeDeserializer(TestTypeResolverBuilder builder,
                                                 AsPropertyTypeDeserializer src, BeanProperty property) {
        super(src, property);
        this.builder = builder;
    }

    @Override
    public Object deserializeTypedFromAny(JsonParser p, DeserializationContext ctxt) throws IOException {
        String typeId = findType(p);
        if (p.hasToken(JsonToken.START_OBJECT) && builder.isCollection(typeId)) {
            JsonDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
            return deser.deserialize(p, ctxt);
        }
        return deserializeTypedFromObject(p, ctxt);
    }

    protected String findType(JsonParser p) {
        if (p instanceof FromXmlParser fromXmlParser) {
            XMLStreamReader xmlStreamReader = fromXmlParser.getStaxReader();
            if (xmlStreamReader.getEventType() == XMLEvent.START_ELEMENT) {
                for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
                    QName qName = xmlStreamReader.getAttributeName(i);
                    String value = xmlStreamReader.getAttributeValue(i);
                    if (_typePropertyName.equals(qName.toString())) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected Object _deserializeTypedForId(JsonParser p, DeserializationContext ctxt, TokenBuffer tb, String typeId) throws IOException {
        JsonDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
        if (_typeIdVisible) {
            if (tb == null) {
                tb = ctxt.bufferForInputBuffering(p);
            }
            tb.writeFieldName(p.currentName());
            tb.writeString(typeId);
        }
        if (deser.handledType() != null && JacksonUtils.isIndexedType(deser.handledType())) {
            tb = ctxt.bufferForInputBuffering(p);
            p.nextToken();
            tb.writeStartObject();
            while (p.currentToken() != JsonToken.END_OBJECT) {
                tb.copyCurrentStructure(p);
                p.nextToken();
            }
            tb.writeEndObject();
        }
        if (tb != null) {
            p.clearCurrentToken();
            p = JsonParserSequence.createFlattened(false, tb.asParser(p), p);
        }
        if (p.currentToken() != JsonToken.END_OBJECT) {
            p.nextToken();
        }
        boolean isWrapped = p.currentToken() == JsonToken.FIELD_NAME && "".equals(p.currentName());
        if (isWrapped) {
            p.nextToken();
        }
        if (p.currentToken() == JsonToken.END_OBJECT && "character".equals(typeId)) {
            return ' ';
        }
        Object bean = deser.deserialize(p, ctxt);
        if (isWrapped && p.currentToken() != JsonToken.END_OBJECT) {
            p.nextToken();
        }
        return bean;
    }

    @Override
    public TypeDeserializer forProperty(BeanProperty prop) {
        return (prop == _property) ? this : new PolymorphicAsPropertyTypeDeserializer(builder, this, prop);
    }
}
