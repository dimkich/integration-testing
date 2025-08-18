package io.github.dimkich.integration.testing.format.xml.tnode;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.sw.BufferingXmlWriter;
import com.ctc.wstx.sw.XmlWriter;
import lombok.SneakyThrows;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.NamespaceStack;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

public class CustomXMLWriter extends XMLWriter {
    private final CharArrayWriter charArrayWriter = new CharArrayWriter();
    private final XmlWriter xmlWriter;
    private final OutputFormat format;
    private final NamespaceStack namespaceStack;
    private final Field indentLevel;

    public CustomXMLWriter(OutputStream out) throws IOException, NoSuchFieldException, IllegalAccessException {
        this(out, DEFAULT_FORMAT);
    }

    public CustomXMLWriter(OutputStream out, OutputFormat format) throws IOException, NoSuchFieldException,
            IllegalAccessException {
        super(out, format);
        this.format = format;
        Field namespaceStackField = XMLWriter.class.getDeclaredField("namespaceStack");
        namespaceStackField.setAccessible(true);
        namespaceStack = (NamespaceStack) namespaceStackField.get(this);
        indentLevel = XMLWriter.class.getDeclaredField("indentLevel");
        indentLevel.setAccessible(true);

        xmlWriter = new BufferingXmlWriter(charArrayWriter, WriterConfig.createFullDefaults(), null, false,
                null, 16);
        xmlWriter.enableXml11();
    }

    protected void writeDeclaration() throws IOException {
        String declaration = CustomSAXContentHandler.getDeclaration();
        if (declaration != null && !format.isSuppressDeclaration()) {
            writer.write(declaration);
            if (format.isNewLineAfterDeclaration()) {
                println();
            }
        } else {
            super.writeDeclaration();
        }
    }

    @SneakyThrows
    protected void writeElement(Element element) {
        int size = element.nodeCount();
        int contentCount = size;
        String qualifiedName = element.getQualifiedName();

        writePrintln();
        indent();

        writer.write("<");
        writer.write(qualifiedName);

        int previouslyDeclaredNamespaces = namespaceStack.size();
        Namespace ns = element.getNamespace();

        if (isNamespaceDeclaration(ns)) {
            namespaceStack.push(ns);
            writeNamespace(ns);
        }
        boolean textOnly = true;

        for (int i = 0; i < size; i++) {
            Node node = element.node(i);
            if (node instanceof Namespace additional) {
                if (isNamespaceDeclaration(additional)) {
                    namespaceStack.push(additional);
                    writeNamespace(additional);
                    contentCount--;
                }
            } else if (node instanceof Element) {
                textOnly = false;
            } else if (node instanceof Comment) {
                textOnly = false;
            }
        }

        writeAttributes(element);

        lastOutputNodeType = Node.ELEMENT_NODE;

        if (contentCount <= 0) {
            writeEmptyElementClose(qualifiedName);
        } else {
            writer.write(">");
            if (textOnly) {
                writeElementContent(element);
            } else {
                indentLevel.set(this, (int) indentLevel.get(this) + 1);
                writeElementContent(element);
                indentLevel.set(this, (int) indentLevel.get(this) - 1);
                writePrintln();
                indent();
            }
            writer.write("</");
            writer.write(qualifiedName);
            writer.write(">");
        }
        while (namespaceStack.size() > previouslyDeclaredNamespaces) {
            namespaceStack.pop();
        }

        lastOutputNodeType = Node.ELEMENT_NODE;
    }

    @Override
    protected void writeElementContent(Element element) throws IOException {
        boolean trimText = format.isTrimText();
        boolean textOnly = true;
        for (Node node : element.content()) {
            if (!(node instanceof Text)) {
                textOnly = false;
                break;
            }
        }
        if (textOnly) {
            format.setTrimText(false);
        }
        try {
            super.writeElementContent(element);
        } finally {
            format.setTrimText(trimText);
        }
    }

    @Override
    @SneakyThrows
    protected String escapeElementEntities(String text) {
        charArrayWriter.reset();
        xmlWriter.writeCharacters(text);
        xmlWriter.flush();
        return charArrayWriter.toString();
    }

    @Override
    @SneakyThrows
    protected void writeAttribute(String qualifiedName, String value) throws IOException {
        charArrayWriter.reset();
        xmlWriter.writeAttribute(qualifiedName, value);
        xmlWriter.flush();
        writer.write(charArrayWriter.toCharArray());
    }
}
