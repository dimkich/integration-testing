package io.github.dimkich.integration.testing.serde.binary;

import java.util.List;

/**
 * Factory for creating {@link BinarySegment}s from format tags in a binary envelope configuration.
 * Providers are registered with {@link BinaryEnvelopeParser} and resolved by tag name
 * (case-insensitive), e.g. {@code {LEN(INT, BE)}} invokes the provider named {@code "LEN"} with
 * params {@code ["INT", "BE"]}.
 *
 * @see BinaryEnvelopeParser
 * @see BinarySegment
 */
public interface BinarySegmentProvider {
    /**
     * Returns the tag name used in the format string (e.g. {@code "LEN"}, {@code "VER"}, {@code "CONTENT"}).
     * Lookup is case-insensitive.
     *
     * @return the provider's tag name
     */
    String getName();

    /**
     * Creates a configured {@link BinarySegment} from the given parameters.
     * Parameters are extracted from the tag's parenthesized list, e.g. {@code {LEN(INT, BE)}} yields
     * {@code ["INT", "BE"]}. Empty or absent parentheses yield an empty list.
     *
     * @param params list of parameter strings; never null
     * @return a configured binary segment for use in the envelope layout
     */
    BinarySegment create(List<String> params);
}
