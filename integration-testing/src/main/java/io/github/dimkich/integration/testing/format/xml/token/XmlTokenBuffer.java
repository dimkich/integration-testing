package io.github.dimkich.integration.testing.format.xml.token;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.lang.reflect.Field;

public class XmlTokenBuffer extends TokenBuffer {
    static final Field parserParsingContextField;

    static {
        try {
            parserParsingContextField = Parser.class.getDeclaredField("_parsingContext");
            parserParsingContextField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public XmlTokenBuffer(JsonParser p, DeserializationContext ctxt) {
        super(p, ctxt);
    }

    @Override
    public JsonParser asParser(ObjectCodec codec) {
        return new XmlJsonParser(super.asParser(codec));
    }

    @Override
    public JsonParser asParser(JsonParser src) {
        return new XmlJsonParser(super.asParser(src));
    }
}