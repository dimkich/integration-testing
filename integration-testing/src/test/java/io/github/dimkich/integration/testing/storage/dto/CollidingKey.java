package io.github.dimkich.integration.testing.storage.dto;

/**
 * Key of a map that always renders the same text, whatever its content.
 */
public class CollidingKey {
    @Override
    public String toString() {
        return "same";
    }
}
