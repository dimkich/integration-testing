package io.github.dimkich.integration.testing.collections.probe;

import java.util.HashMap;

/**
 * Fixture that verifies {@code super()} calls inside classes extending {@link HashMap} are not
 * rewritten by {@code HashCollectionRewriter}.
 */
public class HashMapSubclass extends HashMap<String, String> {

    public HashMapSubclass() {
        super();
        put("x", "y");
    }
}
