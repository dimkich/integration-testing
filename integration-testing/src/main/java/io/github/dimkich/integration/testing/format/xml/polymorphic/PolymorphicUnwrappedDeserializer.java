package io.github.dimkich.integration.testing.format.xml.polymorphic;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.util.Set;

public class PolymorphicUnwrappedDeserializer extends BeanDeserializer {
    private final static String tokenBufferKey = "tokenBuffer";
    private final static String typeBufferKey = "unwrappedTypeBuffer";

    private final SettableBeanProperty unwrappedProperty;
    private final TypeDeserializer unwrappedTypeDeserializer;
    private final String unwrappedTypeProperty;

    public PolymorphicUnwrappedDeserializer(BeanDeserializerBase src, Set<String> ignorableProps,
                                            SettableBeanProperty unwrappedProperty,
                                            TypeDeserializer unwrappedTypeDeserializer,
                                            String unwrappedTypeProperty) {
        super(src, ignorableProps, null);
        this.unwrappedProperty = unwrappedProperty;
        this.unwrappedTypeDeserializer = unwrappedTypeDeserializer;
        this.unwrappedTypeProperty = unwrappedTypeProperty;
    }


    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object bean = super.deserialize(p, ctxt);
        setWrappedProperty(p, ctxt, bean);
        return bean;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object bean) throws IOException {
        bean = super.deserialize(p, ctxt, bean);
        setWrappedProperty(p, ctxt, bean);
        return bean;
    }

    private void setWrappedProperty(JsonParser p, DeserializationContext ctxt, Object bean) throws IOException {
        TokenBuffer typeBuffer = (TokenBuffer) ctxt.getAttribute(typeBufferKey);
        ctxt.setAttribute(typeBufferKey, null);
        TokenBuffer tokenBuffer = (TokenBuffer) ctxt.getAttribute(tokenBufferKey);
        if (tokenBuffer != null) {
            clearTokenBuffer(ctxt);
            p.clearCurrentToken();
            tokenBuffer.writeEndObject();
            JsonParser buffered = tokenBuffer.asParser(p);
            if (typeBuffer != null) {
                buffered = JsonParserSequence.createFlattened(false, typeBuffer.asParser(p), buffered);
            }
            p = JsonParserSequence.createFlattened(false, buffered, p);
            if (p.currentToken() != JsonToken.END_OBJECT) {
                p.nextToken();
            }
            if (unwrappedProperty.getValueDeserializer() != null) {
                p.nextToken();
                Object wrappedBean = unwrappedProperty.getValueDeserializer().deserialize(p, ctxt);
                unwrappedProperty.set(bean, wrappedBean);
            } else if (unwrappedTypeDeserializer == null) {
                JsonDeserializer<Object> deserializer = findDeserializer(ctxt, unwrappedProperty.getType(), unwrappedProperty);
                unwrappedProperty.withValueDeserializer(deserializer).deserializeAndSet(p, ctxt, bean);
            } else {
                Object wrappedBean = unwrappedTypeDeserializer.deserializeTypedFromAny(p, ctxt);
                unwrappedProperty.set(bean, wrappedBean);
            }
        }
    }

    @Override
    protected void handleUnknownProperty(JsonParser p, DeserializationContext ctxt, Object beanOrClass, String propName) throws IOException {
        // The type id may arrive after the payload (a bean with a property-based creator buffers
        // unknown properties and replays them only at the end), so it is captured separately and
        // replayed before the payload instead of relying on the order inside the token buffer.
        if (unwrappedTypeProperty != null && unwrappedTypeProperty.equals(propName)
                && ctxt.getAttribute(typeBufferKey) == null) {
            String typeId = p.getValueAsString();
            if (typeId != null) {
                TokenBuffer typeBuffer = ctxt.bufferForInputBuffering(p);
                typeBuffer.writeFieldName(propName);
                typeBuffer.writeString(typeId);
                ctxt.setAttribute(typeBufferKey, typeBuffer);
                return;
            }
        }
        TokenBuffer tokenBuffer = getTokenBuffer(p, ctxt);
        tokenBuffer.writeFieldName(propName);
        tokenBuffer.copyCurrentStructure(p);
    }

    @Override
    protected void handleIgnoredProperty(JsonParser p, DeserializationContext ctxt, Object beanOrClass, String propName) throws IOException {
        if ("".equals(propName)) {
            getTokenBuffer(p, ctxt).copyCurrentStructure(p);
        } else {
            handleUnknownProperty(p, ctxt, beanOrClass, propName);
        }
    }

    private TokenBuffer getTokenBuffer(JsonParser p, DeserializationContext ctxt) {
        TokenBuffer tokenBuffer = (TokenBuffer) ctxt.getAttribute(tokenBufferKey);
        if (tokenBuffer == null) {
            tokenBuffer = ctxt.bufferForInputBuffering(p);
            ctxt.setAttribute(tokenBufferKey, tokenBuffer);
        }
        return tokenBuffer;
    }

    private void clearTokenBuffer(DeserializationContext ctxt) {
        ctxt.setAttribute(tokenBufferKey, null);
    }
}
