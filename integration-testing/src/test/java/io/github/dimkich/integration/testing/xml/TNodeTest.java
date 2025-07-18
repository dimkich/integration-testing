package io.github.dimkich.integration.testing.xml;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TNodeTest {

    static Object[][] findData() {
        return new Object[][]{
                {"<t/>", find(n -> n.findChildNode("t")), "<t/>"},
                {"<t/>", find(n -> n.getAttributeValue("a")), null},
                {"<t/>", find(n -> n.findChildNode("t").getAttributeValue("a")), null},
                {"<t>tx</t>", find(n -> n.findChildNode("t").getValue()), "tx"},
                {"<t a=\"1\"/>", find(n -> n.findChildNode("t").getAttributeValue("a")), "1"},
                {"<r><a><b/></a><b/><b/></r>", find(TNode::findNodes),
                        "<r><a><b/></a><b/><b/></r>;<a><b/></a>;<b/>;<b/>;<b/>"},
                {"<r><a><b/></a><b/><b/></r>", find(n -> n.findNodes("b")), "<b/>;<b/>;<b/>"},
                {"<r><a><b/></a><b/><b/></r>", find(n -> n.findChildNode("r").findChildNode("b")),
                        "<b/>"},
                {"<r><a><b/></a><b/><b/></r>", find(n -> n.findChildNode("r").findChildNodes("b")),
                        "<b/>;<b/>"},
        };
    }

    @ParameterizedTest
    @MethodSource("findData")
    void find(String xml, Function<TNode, Object> function, String expected) {
        TNode root = create(xml);
        Object result = function.apply(root);
        if (result instanceof TNode node) {
            result = toString(node);
        } else if (result instanceof Stream<?> stream) {
            result = stream.map(n -> (TNode) n).map(this::toString).collect(Collectors.joining(";"));
        }
        assertEquals(expected, result != null ? result.toString() : null);
    }

    static Object[][] changeData() {
        return new Object[][]{
                {"<t/>", set(n -> fc(n).setAttributeValue("a", "1")), "<t a=\"1\"/>"},
                {"<t a=\"1\"/>", set(n -> fc(n).setAttributeValue("a", null)), "<t/>"},
                {"<t/>", set(n -> fc(n).setValue("text")), "<t>text</t>"},
                {"<t><a/>text<b/></t>", set(n -> fc(n).setValue(null)), "<t><a/><b/></t>"},
                {"<t><a/><b/></t>", set(n -> fc(n).findChildNode("b").remove()), "<t><a/></t>"},
                {"<t/>", set(n -> fc(n).addChild("w")), "<t><w/></t>"},
                {"<t/>", set(n -> fc(n).addChild("w", "tx")), "<t><w>tx</w></t>"},
                {"<t a1=\"e\"/>", set(n -> fc(n).setName("w")), "<w a1=\"e\"/>"},
                {"<t/>", set(n -> fc(n).setChildNodes(fc(create("<x><a/><b/><c/></x>")).getChildNodes().toList())),
                        "<t><a/><b/><c/></t>"},
        };
    }

    @ParameterizedTest
    @MethodSource("changeData")
    void change(String xml, Consumer<TNode> consumer, String expected) {
        TNode root = create(xml);
        consumer.accept(root);
        assertEquals(expected, toString(fc(root)));
    }

    private static TNode fc(TNode node) {
        return node.getChildNodes().findFirst().orElseThrow(() -> new RuntimeException("No child node found"));
    }

    private static Function<TNode, Object> find(Function<TNode, Object> function) {
        return function;
    }

    private static Consumer<TNode> set(Consumer<TNode> consumer) {
        return consumer;
    }

    private static TNode create(String text) {
        return TNode.create(new ByteArrayInputStream(text.getBytes()));
    }

    private String toString(TNode node) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        node.save(stream, false);
        return stream.toString();
    }
}