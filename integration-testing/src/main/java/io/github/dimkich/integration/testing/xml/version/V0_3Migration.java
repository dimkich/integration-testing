package io.github.dimkich.integration.testing.xml.version;

import io.github.dimkich.integration.testing.util.TestUtils;
import io.github.dimkich.integration.testing.xml.tnode.TNode;

public class V0_3Migration implements XmlMigration {
    private boolean changed;

    @Override
    public void migrate(String path) {
        TNode node = TNode.create(TestUtils.getTestResourceFile(path));
        changed = false;
        node.findNodes("testCase")
                .forEach(n -> {
                    changed = true;
                    n.setName("test");
                    if (n.findChildNodes("testCase").toList().isEmpty()) {
                        n.setAttributeValue("type", "case");
                    } else {
                        n.setAttributeValue("type", "container");
                    }
                });
        if (changed) {
            node.save(TestUtils.getTestResourceFile(path));
        }
    }
}
