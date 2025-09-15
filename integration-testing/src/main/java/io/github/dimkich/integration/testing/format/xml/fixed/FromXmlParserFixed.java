package io.github.dimkich.integration.testing.format.xml.fixed;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.XmlNameProcessor;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

public class FromXmlParserFixed extends FromXmlParser {
    private boolean xsiNil = false;

    public FromXmlParserFixed(IOContext ctxt, int genericParserFeatures, int xmlFeatures, ObjectCodec codec,
                              XMLStreamReader xmlReader, XmlNameProcessor tagProcessor) throws IOException {
        super(ctxt, genericParserFeatures, xmlFeatures, codec, xmlReader, tagProcessor);
    }

    @Override
    public JsonToken nextToken() throws IOException {
        xsiNil = _xmlTokens.hasXsiNil();
        super.nextToken();
        if (xsiNil && _currToken == JsonToken.START_OBJECT && _nextToken == JsonToken.END_OBJECT) {
            super.nextToken();
            _currToken = JsonToken.VALUE_NULL;
        } else if (_currToken == JsonToken.VALUE_NULL && !xsiNil) {
            _nextToken = JsonToken.END_OBJECT;
            _currToken = JsonToken.START_OBJECT;
        }
        return _currToken;
    }

    @Override
    protected int _nextToken() throws IOException {
        int t = super._nextToken();
        if (_xmlTokens.hasXsiNil()) {
            xsiNil = true;
        }
        return t;
    }

    @Override
    public boolean isExpectedStartArrayToken() {
        try {
            return super.isExpectedStartArrayToken();
        } catch (IllegalStateException e) {
            if (_currToken == JsonToken.START_ARRAY && _nextToken == JsonToken.END_ARRAY) {
                return true;
            }
            throw e;
        }
    }
}
