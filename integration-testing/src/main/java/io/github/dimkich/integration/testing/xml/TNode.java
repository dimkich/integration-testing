package io.github.dimkich.integration.testing.xml;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TNode {
    private final Node node;

    @SneakyThrows
    public static TNode create(File file) {
        SAXReader reader = new SAXReader();
        Document document = reader.read(file);
        return new TNode(document);
    }

    @SneakyThrows
    public static TNode create(InputStream stream) {
        SAXReader reader = new SAXReader();
        Document document = reader.read(stream);
        return new TNode(document);
    }

    public TNode findChildNode(String name) {
        return getChildNodes()
                .filter(n2 -> n2.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No such node: " + name));
    }

    public Stream<TNode> findChildNodes(String name) {
        return getChildNodes().filter(n -> n.getName().equals(name));
    }

    public TNode addChild(String name) {
        return new TNode(((Element) node).addElement(name));
    }

    public TNode addChild(String tag, String value) {
        Element element = ((Element) node).addElement(tag);
        element.setText(value);
        return new TNode(element);
    }

    public void remove() {
        node.getParent().remove(node);
    }

    public String getName() {
        return node.getName();
    }

    public TNode setName(String newName) {
        node.setName(newName);
        return this;
    }

    public String getValue() {
        return node.getText().isEmpty() ? null : node.getText();
    }

    public TNode setValue(String value) {
        if (value == null) {
            for (Iterator<Node> iterator = ((Branch) node).nodeIterator(); iterator.hasNext(); ) {
                Node node = iterator.next();
                if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                    iterator.remove();
                }
            }
            return this;
        }
        node.setText(value);
        return this;
    }

    public String getAttributeValue(String name) {
        if (node instanceof Element element) {
            for (Attribute attribute : element.attributes()) {
                if (attribute.getName().equals(name)) {
                    return attribute.getValue();
                }
            }
        }
        return null;
    }

    public TNode setAttributeValue(String name, String value) {
        ((Element) node).addAttribute(name, value);
        return this;
    }

    public Stream<TNode> getChildNodes() {
        if (node instanceof Branch branch) {
            return branch.content().stream()
                    .filter(n -> n instanceof Element)
                    .map(TNode::new);
        }
        return Stream.of();
    }

    public TNode setChildNodes(Collection<TNode> newChildNodes) {
        ((Branch) node).setContent(newChildNodes.stream().map(tn -> tn.node).toList());
        return this;
    }

    public Stream<TNode> findNodes() {
        if (node instanceof Branch branch) {
            return branch.content().stream()
                    .filter(n -> n instanceof Element)
                    .map(TNode::new)
                    .flatMap(n -> Stream.concat(Stream.of(n), n.findNodes()));
        }
        return Stream.of();
    }

    public Stream<TNode> findNodes(String name) {
        return findNodes().filter(n -> n.getName().equals(name));
    }

    @SneakyThrows
    public void save(File file, boolean prettyPrint) {
        save(new FileOutputStream(file), prettyPrint);
    }

    @SneakyThrows
    public void save(OutputStream stream, boolean prettyPrint) {
        XMLWriter writer;
        if (prettyPrint) {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setIndentSize(4);
            format.setNewLineAfterDeclaration(false);
            format.setLineSeparator(System.lineSeparator());
            writer = new XMLWriter(stream, format);
        } else {
            writer = new XMLWriter(stream);
        }
        writer.write(node);
    }
}
