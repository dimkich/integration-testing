package io.github.dimkich.integration.testing.format.xml.fixed;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.XmlNameProcessor;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.XmlTokenStream;
import io.github.dimkich.integration.testing.format.xml.AttributeAwareParser;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

public class FromXmlParserFixed extends FromXmlParser implements AttributeAwareParser {
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

    public String getRawText() {
        return _currText;
    }

    /**
     * Reports whether the current field name is an XML attribute rather than a child element.
     * <p>
     * Needed by deserializers that render both attributes and children as fields, for example the
     * storage difference reader where {@code key} and {@code type} are attributes but a field of a
     * record may also be called {@code key}.
     *
     * @return {@code true} when the current token is an attribute name
     */
    @Override
    public boolean isCurrentTokenAttribute() {
        return _xmlTokens.getCurrentToken() == XmlTokenStream.XML_ATTRIBUTE_NAME;
    }
}
