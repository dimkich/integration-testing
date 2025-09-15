package io.github.dimkich.integration.testing.format.xml.fixed;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.util.StaxUtil;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.io.Stax2ByteArraySource;
import org.codehaus.stax2.io.Stax2CharArraySource;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;

public class XmlFactoryFixed extends XmlFactory {
    @Override
    public FromXmlParser createParser(XMLStreamReader sr) throws IOException {
        // note: should NOT move parser if already pointing to START_ELEMENT
        if (sr.getEventType() != XMLStreamConstants.START_ELEMENT) {
            sr = _initializeXmlReader(sr);
        }

        // false -> not managed
        FromXmlParser xp = new FromXmlParserFixed(_createContext(_createContentReference(sr), false),
                _parserFeatures, _xmlParserFeatures, _objectCodec, sr, _nameProcessor);
        if (_cfgNameForTextElement != null) {
            xp.setXMLTextElementName(_cfgNameForTextElement);
        }
        return xp;
    }

    @Override
    protected FromXmlParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        XMLStreamReader sr;
        try {
            sr = _xmlInputFactory.createXMLStreamReader(in);
        } catch (XMLStreamException e) {
            return StaxUtil.throwAsParseException(e, null);
        }
        sr = _initializeXmlReader(sr);
        FromXmlParser xp = new FromXmlParserFixed(ctxt, _parserFeatures, _xmlParserFeatures,
                _objectCodec, sr, _nameProcessor);
        if (_cfgNameForTextElement != null) {
            xp.setXMLTextElementName(_cfgNameForTextElement);
        }
        return xp;
    }

    @Override
    protected FromXmlParser _createParser(Reader r, IOContext ctxt) throws IOException {
        XMLStreamReader sr;
        try {
            sr = _xmlInputFactory.createXMLStreamReader(r);
        } catch (XMLStreamException e) {
            return StaxUtil.throwAsParseException(e, null);
        }
        sr = _initializeXmlReader(sr);
        FromXmlParser xp = new FromXmlParserFixed(ctxt, _parserFeatures, _xmlParserFeatures,
                _objectCodec, sr, _nameProcessor);
        if (_cfgNameForTextElement != null) {
            xp.setXMLTextElementName(_cfgNameForTextElement);
        }
        return xp;
    }

    @Override
    protected FromXmlParser _createParser(char[] data, int offset, int len, IOContext ctxt,
                                          boolean recycleBuffer) throws IOException {
        // !!! TODO: add proper handling of 'recycleBuffer'; currently its handling
        //    is always same as if 'false' was passed
        XMLStreamReader sr;
        try {
            // 03-Jul-2021, tatu: [dataformat-xml#482] non-Stax2 impls unlikely to
            //    support so avoid:
            if (_xmlInputFactory instanceof XMLInputFactory2) {
                sr = _xmlInputFactory.createXMLStreamReader(new Stax2CharArraySource(data, offset, len));
            } else {
                sr = _xmlInputFactory.createXMLStreamReader(new CharArrayReader(data, offset, len));
            }
        } catch (XMLStreamException e) {
            return StaxUtil.throwAsParseException(e, null);
        }
        sr = _initializeXmlReader(sr);
        FromXmlParser xp = new FromXmlParserFixed(ctxt, _parserFeatures, _xmlParserFeatures,
                _objectCodec, sr, _nameProcessor);
        if (_cfgNameForTextElement != null) {
            xp.setXMLTextElementName(_cfgNameForTextElement);
        }
        return xp;
    }

    @Override
    protected FromXmlParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        XMLStreamReader sr;
        try {
            // 03-Jul-2021, tatu: [dataformat-xml#482] non-Stax2 impls unlikely to
            //    support so avoid:
            if (_xmlInputFactory instanceof XMLInputFactory2) {
                sr = _xmlInputFactory.createXMLStreamReader(new Stax2ByteArraySource(data, offset, len));
            } else {
                sr = _xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(data, offset, len));
            }
        } catch (XMLStreamException e) {
            return StaxUtil.throwAsParseException(e, null);
        }
        sr = _initializeXmlReader(sr);
        FromXmlParser xp = new FromXmlParserFixed(ctxt, _parserFeatures, _xmlParserFeatures,
                _objectCodec, sr, _nameProcessor);
        if (_cfgNameForTextElement != null) {
            xp.setXMLTextElementName(_cfgNameForTextElement);
        }
        return xp;
    }
}
