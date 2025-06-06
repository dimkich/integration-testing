package io.github.dimkich.integration.testing.xml;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TNode {
    private final Document document;
    private final Node node;

    @SneakyThrows
    public static TNode create(File file) {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(file);
        return new TNode(document, document);
    }

    @SneakyThrows
    public static TNode create(InputStream stream) {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(stream);
        return new TNode(document, document);
    }

    public TNode createTextNode(String text) {
        return new TNode(document, document.createTextNode(text));
    }

    public TNode createNode(String tag, String value) {
        Element element = document.createElement(tag);
        element.appendChild(document.createTextNode(value));
        return new TNode(document, element);
    }

    public String getName() {
        return node.getNodeName();
    }

    public TNode setName(String newName) {
        document.renameNode(node, null, newName);
        return this;
    }

    public String getValue() {
        return switch (node.getNodeType()) {
            case Node.ELEMENT_NODE -> node.getFirstChild().getTextContent();
            case Node.TEXT_NODE -> node.getNodeValue();
            default -> throw new RuntimeException();
        };
    }

    public TNode setValue(String value) {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE -> node.getFirstChild().setTextContent(value);
            case Node.TEXT_NODE -> node.setNodeValue(value);
            default -> throw new RuntimeException();
        }
        return this;
    }

    public String getAttributeValue(String name) {
        return node.getAttributes() == null ? null : node.getAttributes().getNamedItem(name) == null
                ? null : node.getAttributes().getNamedItem(name).getNodeValue();
    }

    public TNode setAttributeValue(String name, String value) {
        Node attrNode = node.getAttributes().getNamedItem(name);
        if (attrNode == null) {
            ((Element) node).setAttribute(name, value);
        } else {
            attrNode.setNodeValue(value);
        }
        return this;
    }

    public List<TNode> getChildNodes() {
        NodeList nodeList = node.getChildNodes();
        return IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .map(n -> new TNode(document, n))
                .toList();
    }

    public TNode setChildNodes(List<TNode> nodes) {
        while (node.getChildNodes().getLength() > 0) {
            node.removeChild(node.getFirstChild());
        }
        for (TNode tNode : nodes) {
            node.appendChild(tNode.node);
        }
        return this;
    }

    public Stream<TNode> stream() {
        return getChildNodes().stream()
                .flatMap(n -> Stream.concat(Stream.of(n), n.stream()));
    }

    public void save(File file) {
        save(new StreamResult(file));
    }

    public void save(OutputStream stream) {
        save(new StreamResult(stream));
    }

    @SneakyThrows
    private void save(StreamResult streamResult) {
        DOMSource dom = new DOMSource(document);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(dom, streamResult);
    }
}
