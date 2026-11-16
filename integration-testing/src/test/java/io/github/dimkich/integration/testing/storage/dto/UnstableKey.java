package io.github.dimkich.integration.testing.storage.dto;

/**
 * Key of a map without its own {@code toString()}: the default identifier would be an identity
 * hash, so the difference engine must reject it.
 */
public class UnstableKey {
}
