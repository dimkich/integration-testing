package io.github.dimkich.integration.testing.xml.tnode;

import lombok.SneakyThrows;
import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class CustomSAXContentHandler extends SAXContentHandler {
    private static final ThreadLocal<String> declaration = new ThreadLocal<>();
    private Locator locator;
    private String currentLocation;

    private final CustomElementHandler elementHandler;

    @SneakyThrows
    public CustomSAXContentHandler(DocumentFactory documentFactory, CustomElementHandler elementHandler) {
        super(documentFactory, elementHandler);
        this.elementHandler = elementHandler;
    }

    @Override
    public void declaration(String version, String encoding, String standalone) {
        declaration.set("<?xml "
                + (version != null ? "version='" + version + "'": "")
                + (encoding != null ? " encoding='" + encoding + "'": "")
                + (standalone != null ? " standalone='" + standalone + "'": "")
                + "?>");
    }

    public static String getDeclaration() {
        return declaration.get();
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attributes) throws SAXException {
        super.startElement(namespaceURI, localName, qualifiedName, attributes);
        currentLocation = locator.getPublicId() + ":" + locator.getSystemId() + ":" + locator.getLineNumber() + ":"
                + locator.getColumnNumber();
    }

    @Override
    @SneakyThrows
    public void endElement(String namespaceURI, String localName, String qName) {
        super.endElement(namespaceURI, localName, qName);
        String location = locator.getPublicId() + ":" + locator.getSystemId() + ":" + locator.getLineNumber() + ":"
                + locator.getColumnNumber();
        if (elementHandler.isStartAndEndElementsEquals() && !currentLocation.equals(location)
                && elementHandler.getEndElement().content().isEmpty()) {
            elementHandler.getEndElement().setText("");
        }
    }

    @Override
    public void setDocumentLocator(Locator documentLocator) {
        super.setDocumentLocator(documentLocator);
        this.locator = documentLocator;
    }
}
