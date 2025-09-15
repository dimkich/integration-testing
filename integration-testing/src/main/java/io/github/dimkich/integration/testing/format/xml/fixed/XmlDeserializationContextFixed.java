package io.github.dimkich.integration.testing.format.xml.fixed;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import io.github.dimkich.integration.testing.format.xml.token.XmlTokenBuffer;

import java.io.IOException;

public class XmlDeserializationContextFixed extends DefaultDeserializationContext {
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle methods
    /**********************************************************
     */

    /**
     * Default constructor for a blueprint object, which will use the standard
     * {@link DeserializerCache}, given factory.
     */
    public XmlDeserializationContextFixed(DeserializerFactory df) {
        super(df, null);
    }

    private XmlDeserializationContextFixed(XmlDeserializationContextFixed src,
                                           DeserializationConfig config, JsonParser p, InjectableValues values) {
        super(src, config, p, values);
    }

    private XmlDeserializationContextFixed(XmlDeserializationContextFixed src) {
        super(src);
    }

    private XmlDeserializationContextFixed(XmlDeserializationContextFixed src, DeserializerFactory factory) {
        super(src, factory);
    }

    private XmlDeserializationContextFixed(XmlDeserializationContextFixed src, DeserializationConfig config) {
        super(src, config);
    }

    @Override
    public TokenBuffer bufferForInputBuffering(JsonParser p) {
        return new XmlTokenBuffer(p, this);
    }

    @Override
    public XmlDeserializationContextFixed copy() {
        return new XmlDeserializationContextFixed(this);
    }

    @Override
    public DefaultDeserializationContext createInstance(DeserializationConfig config,
                                                        JsonParser p, InjectableValues values) {
        return new XmlDeserializationContextFixed(this, config, p, values);
    }

    @Override
    public DefaultDeserializationContext createDummyInstance(DeserializationConfig config) {
        // need to be careful to create non-blue-print instance
        return new XmlDeserializationContextFixed(this, config);
    }

    @Override
    public DefaultDeserializationContext with(DeserializerFactory factory) {
        return new XmlDeserializationContextFixed(this, factory);
    }

    /*
    /**********************************************************
    /* Overrides we need
    /**********************************************************
     */

    @Override // since 2.12
    public Object readRootValue(JsonParser p, JavaType valueType,
                                JsonDeserializer<Object> deser, Object valueToUpdate)
            throws IOException {
        // 18-Sep-2021, tatu: Complicated mess; with 2.12, had [dataformat-xml#374]
        //    to disable handling. With 2.13, via [dataformat-xml#485] undid this change
        if (_config.useRootWrapping()) {
            return _unwrapAndDeserialize(p, valueType, deser, valueToUpdate);
        }
        if (valueToUpdate == null) {
            return deser.deserialize(p, this);
        }
        return deser.deserialize(p, this, valueToUpdate);
    }

    // To support case where XML element has attributes as well as CDATA, need
    // to "extract" scalar value (CDATA), after the fact
    @Override // since 2.12
    public String extractScalarFromObject(JsonParser p, JsonDeserializer<?> deser,
                                          Class<?> scalarType)
            throws IOException {
        // Only called on START_OBJECT, should not need to check, but JsonParser we
        // get may or may not be `FromXmlParser` so traverse using regular means
        String text = "";

        while (p.nextToken() == JsonToken.FIELD_NAME) {
            // Couple of ways to find "real" textual content. One is to look for
            // "XmlText"... but for that would need to know configuration. Alternatively
            // could hold on to last text seen -- but this might be last attribute, for
            // empty element. So for now let's simply hard-code check for empty String
            // as marker and hope for best
            final String propName = p.currentName();
            JsonToken t = p.nextToken();
            if (t == JsonToken.VALUE_STRING) {
                if (propName.isEmpty()) {
                    text = p.getText();
                }
            } else {
                p.skipChildren();
            }
        }
        return text;
    }

}
