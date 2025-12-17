package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.dataformat.xml.deser.WrapperHandlingDeserializer;
import io.github.dimkich.integration.testing.format.util.JacksonUtils;

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
            JsonDeserializer<Object> valueDeserializer = prop.getValueDeserializer();
            if (valueDeserializer.handledType() == null || !JacksonUtils.isIndexedType(valueDeserializer.handledType())) {
                continue;
            }
            JsonInclude include = prop.getAnnotation(JsonInclude.class);
            if (include != null && include.value() == JsonInclude.Include.ALWAYS) {
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
