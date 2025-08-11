package io.github.dimkich.integration.testing.xml.tnode;

import lombok.AccessLevel;
import lombok.Cleanup;
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
        SAXReader reader = new CustomSAXReader();
        Document document = reader.read(file);
        return new TNode(document);
    }

    @SneakyThrows
    public static TNode create(InputStream stream) {
        SAXReader reader = new CustomSAXReader();
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

    public TNode addNamespace(String prefix, String uri) {
        ((Element) node).addNamespace(prefix, uri);
        return this;
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
    public void save(File file) {
        save(new FileOutputStream(file));
    }

    @SneakyThrows
    public void save(OutputStream stream) {
        OutputFormat format = new OutputFormat();
        format.setIndentSize(4);
        format.setNewlines(true);
        format.setTrimText(true);
        format.setNewLineAfterDeclaration(false);
        format.setLineSeparator(System.lineSeparator());
        format.setAttributesOrderComparator((a1, a2) -> {
            if ("type".equals(a1.getName())) {
                return -1;
            }
            if ("type".equals(a2.getName())) {
                return 1;
            }
            return 0;
        });
        @Cleanup XMLWriter writer = new CustomXMLWriter(stream, format);
        writer.write(node);
        stream.close();
    }
}
