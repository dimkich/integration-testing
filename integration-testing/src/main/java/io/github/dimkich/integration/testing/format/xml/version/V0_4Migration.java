package io.github.dimkich.integration.testing.format.xml.version;

import io.github.dimkich.integration.testing.format.xml.tnode.TNode;
import io.github.dimkich.integration.testing.util.TestUtils;

public class V0_4Migration implements XmlMigration {
    private boolean changed;

    @Override
    public void migrate(String path) {
        TNode node = TNode.create(TestUtils.getTestResourceFile(path));
        changed = false;
        node.findNodes()
                .filter(n -> "string".equals(n.getAttributeValue("type")))
                .forEach(n -> {
                    changed = true;
                    n.setAttributeValue("type", null);
                });
        if (changed) {
            node.save(TestUtils.getTestResourceFile(path));
        }
    }
}