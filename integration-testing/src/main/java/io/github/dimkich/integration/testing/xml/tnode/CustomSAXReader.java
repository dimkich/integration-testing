package io.github.dimkich.integration.testing.xml.tnode;

import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;
import org.xml.sax.XMLReader;

public class CustomSAXReader extends SAXReader {
    @Override
    protected SAXContentHandler createContentHandler(XMLReader reader) {
        return new CustomSAXContentHandler(getDocumentFactory(), new CustomElementHandler());
    }
}
