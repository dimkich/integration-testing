package io.github.dimkich.integration.testing.format.xml.version;

import io.github.dimkich.integration.testing.format.xml.tnode.TNode;
import io.github.dimkich.integration.testing.util.TestUtils;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Flattens legacy stub call results without touching the rest of the document.
 * <p>
 * {@code <result><return>...</return></result>} becomes {@code <result>...</result>}
 * and {@code <result><throw type="..."/></result>} becomes {@code <result type="..."/>}.
 * A throwable result is still recognized by its type at runtime.
 */
public class V0_4StubResultMigration implements XmlMigration {
    private static final String STUB_CALL_ELEMENT = "stubCall";
    private static final String RESULT_ELEMENT = "result";
    private static final Set<String> RESULT_VALUE_ELEMENTS = Set.of("return", "throw");

    @Override
    public void migrate(String path) {
        migrate(TestUtils.getTestResourceFile(path));
    }

    public void migrate(File file) {
        TNode node = TNode.create(file);
        if (migrate(node)) {
            node.save(file);
        }
    }

    /**
     * Applies the stub result migration to the given document root.
     *
     * @param root the root node of the test XML document
     * @return {@code true} if the document was changed and has to be saved
     */
    public boolean migrate(TNode root) {
        boolean changed = false;
        for (TNode stubCall : root.findNodes(STUB_CALL_ELEMENT).toList()) {
            for (TNode result : stubCall.findChildNodes(RESULT_ELEMENT).toList()) {
                if (flatten(result)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean flatten(TNode result) {
        List<TNode> children = result.getChildNodes().toList();
        if (children.size() != 1 || !RESULT_VALUE_ELEMENTS.contains(children.get(0).getName())) {
            return false;
        }
        TNode value = children.get(0);
        String text = value.getValue();
        List<TNode> valueChildren = value.getChildNodes().toList();
        value.getAttributes().forEach(result::setAttributeValue);
        valueChildren.forEach(TNode::remove);
        value.remove();
        result.setValue(text);
        if (!valueChildren.isEmpty()) {
            result.setChildNodes(valueChildren);
        }
        return true;
    }

    public static void main(String[] args) {
        new V0_4StubResultMigration().migrate(args[0]);
    }
}
