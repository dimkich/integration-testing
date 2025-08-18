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
import io.github.dimkich.integration.testing.format.xml.token.XmlTokenBuffer;

import java.io.IOException;
import java.util.Set;

public class PolymorphicUnwrappedDeserializer extends BeanDeserializer {
    private final static String tokenBufferKey = "tokenBuffer";

    private final SettableBeanProperty unwrappedProperty;
    private final TypeDeserializer unwrappedTypeDeserializer;

    public PolymorphicUnwrappedDeserializer(BeanDeserializerBase src, Set<String> ignorableProps,
                                            SettableBeanProperty unwrappedProperty, TypeDeserializer unwrappedTypeDeserializer) {
        super(src, ignorableProps, null);
        this.unwrappedProperty = unwrappedProperty;
        this.unwrappedTypeDeserializer = unwrappedTypeDeserializer;
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
        TokenBuffer tokenBuffer = (TokenBuffer) ctxt.getAttribute(tokenBufferKey);
        if (tokenBuffer != null) {
            clearTokenBuffer(ctxt);
            p.clearCurrentToken();
            tokenBuffer.writeEndObject();
            p = JsonParserSequence.createFlattened(false, tokenBuffer.asParser(p), p);
            if (p.currentToken() != JsonToken.END_OBJECT) {
                p.nextToken();
            }
            if (unwrappedTypeDeserializer == null) {
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
            tokenBuffer = new XmlTokenBuffer(p, ctxt);
            ctxt.setAttribute(tokenBufferKey, tokenBuffer);
        }
        return tokenBuffer;
    }

    private void clearTokenBuffer(DeserializationContext ctxt) {
        ctxt.setAttribute(tokenBufferKey, null);
    }
}
