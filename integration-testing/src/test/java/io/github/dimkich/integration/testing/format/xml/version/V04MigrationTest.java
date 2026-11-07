package io.github.dimkich.integration.testing.format.xml.version;

import io.github.dimkich.integration.testing.format.xml.tnode.TNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V04MigrationTest {
    private final V0_4Migration migration = new V0_4Migration();

    @Test
    void renamesTablesToChange() {
        TNode root = create("<test><init type=\"SqlStorageInit\">"
                + "<tablesToChange>t1,t2</tablesToChange>"
                + "</init></test>");

        assertTrue(migration.migrate(root));
        assertEquals(List.of("t1,t2"), values(root, "tablesToClear"));
        assertEquals(List.of(), values(root, "tablesToChange"));
        assertFalse(migration.migrate(root));
    }

    @Test
    void mergesTablesToChangeAndTablesToClear() {
        TNode root = create("<test><init type=\"SqlStorageInit\">"
                + "<tablesToChange>t2, t3</tablesToChange>"
                + "<tablesToClear>t1,t2</tablesToClear>"
                + "</init></test>");

        assertTrue(migration.migrate(root));
        assertEquals(List.of("t1,t2,t3"), values(root, "tablesToClear"));
        assertEquals(List.of(), values(root, "tablesToChange"));
        assertFalse(migration.migrate(root));
    }

    private static TNode create(String xml) {
        return TNode.create(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<String> values(TNode root, String name) {
        return root.findNodes(name).map(TNode::getValue).toList();
    }
}
