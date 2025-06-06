package io.github.dimkich.integration.testing.xml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.dataformat.xml.deser.WrapperHandlingDeserializer;
import com.fasterxml.jackson.dataformat.xml.util.TypeUtil;
import io.github.dimkich.integration.testing.xml.token.XmlJsonParser;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WrapperHandlingDeserializerFixed extends WrapperHandlingDeserializer {
    public WrapperHandlingDeserializerFixed(BeanDeserializerBase delegate) {
        super(delegate);
    }

    public WrapperHandlingDeserializerFixed(BeanDeserializerBase delegate, Set<String> namesToWrap) {
        super(delegate, namesToWrap);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        _configureParser(p);
        configureTokenParser(p);
        return _delegatee.deserialize(p, ctxt);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt,
                              Object intoValue) throws IOException {
        _configureParser(p);
        configureTokenParser(p);
        return ((JsonDeserializer<Object>) _delegatee).deserialize(p, ctxt, intoValue);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
                                      TypeDeserializer typeDeserializer) throws IOException {
        _configureParser(p);
        configureTokenParser(p);
        return _delegatee.deserializeWithType(p, ctxt, typeDeserializer);
    }

    private void configureTokenParser(JsonParser p) {
        while (p instanceof JsonParserDelegate) {
            if (p instanceof XmlJsonParser xmlJsonParser) {
                JsonToken t = p.currentToken();
                if (t == JsonToken.START_OBJECT || t == JsonToken.START_ARRAY
                        || t == JsonToken.FIELD_NAME) {
                    xmlJsonParser.addVirtualWrapping(_namesToWrap, _caseInsensitive);
                }
                return;
            }
            p = ((JsonParserDelegate) p).delegate();
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType vt = _type;
        if (vt == null) {
            vt = ctxt.constructType(_delegatee.handledType());
        }
        JsonDeserializer<?> del = ctxt.handleSecondaryContextualization(_delegatee, property, vt);
        BeanDeserializerBase newDelegatee = _verifyDeserType(del);

        // Let's go through the properties now...
        Iterator<SettableBeanProperty> it = newDelegatee.properties();
        HashSet<String> unwrappedNames = null;
        while (it.hasNext()) {
            SettableBeanProperty prop = it.next();
            // First things first: only consider array/Collection types
            // (not perfect check, but simplest reasonable check)
            JavaType type = prop.getType();
            JsonDeserializer<Object> valueDeserializer = prop.getValueDeserializer();
            JavaType convertedType = null;
            if (valueDeserializer instanceof StdDelegatingDeserializer<Object> delegatingDeserializer) {
                convertedType = delegatingDeserializer.getValueType();
            }
            if (!TypeUtil.isIndexedType(type) && (convertedType == null || !TypeUtil.isIndexedType(convertedType))) {
                continue;
            }
            PropertyName wrapperName = prop.getWrapperName();
            // skip anything with wrapper (should work as is)
            if ((wrapperName != null) && (wrapperName != PropertyName.NO_NAME)) {
                continue;
            }
            if (unwrappedNames == null) {
                unwrappedNames = new HashSet<>();
            }
            // not optimal; should be able to use PropertyName...
            unwrappedNames.add(prop.getName());
            for (PropertyName alias : prop.findAliases(ctxt.getConfig())) {
                unwrappedNames.add(alias.getSimpleName());
            }
        }
        // Ok: if nothing to take care of, just return the delegatee...
        if (unwrappedNames == null) {
            return newDelegatee;
        }
        // Otherwise, create the thing that can deal with virtual wrapping
        return new WrapperHandlingDeserializerFixed(newDelegatee, unwrappedNames);
    }
}
