package io.github.dimkich.integration.testing.serde.binary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Parses a binary envelope configuration string (hex literals and named tags like
 * {@code {LEN(INT, BE)}}) into a sequence of {@link BinarySegment}s. Tags are looked up in the
 * configured {@link BinarySegmentProvider}s by name (case-insensitive); parsed layouts are
 * cached per configuration string. The format syntax is described in
 * {@code docs/{ru,en}/serde/Binary-Envelopes.md}.
 */
public class BinaryEnvelopeParser {
    private final Map<String, BinarySegmentProvider> providerMap;
    private final Map<String, List<BinarySegment>> layoutCache = new ConcurrentHashMap<>();

    /**
     * Creates a parser with the given segment providers.
     *
     * @param providers list of providers; each tag name must be unique (case-insensitive)
     * @throws IllegalStateException if two providers share the same tag name
     */
    public BinaryEnvelopeParser(List<BinarySegmentProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.getName().toUpperCase(),
                        p -> p,
                        (existing, replacement) -> {
                            throw new IllegalStateException("Duplicate provider for tag: " + existing.getName());
                        }
                ));
    }

    /**
     * Parses the binary envelope configuration into a list of segments.
     * Results are cached; repeated calls with the same config return the same list.
     *
     * @param config format string (e.g. {@code {LEN}{CONTENT}} or {@code AA{STR(12)}{CONTENT}{FIX(2)}})
     * @return ordered list of binary segments; empty list if config is null or blank
     * @throws IllegalArgumentException if an unknown tag name is encountered
     * @throws RuntimeException if the config string has invalid syntax
     */
    public List<BinarySegment> parse(String config) {
        if (config == null || config.isBlank()) {
            return List.of();
        }
        return layoutCache.computeIfAbsent(config, this::doParse);
    }

    private List<BinarySegment> doParse(String config) {
        BinaryEnvelopeTokenizer tokenizer = new BinaryEnvelopeTokenizer(config);
        List<BinarySegment> segments = new ArrayList<>();

        while (tokenizer.peekToken() != BinaryEnvelopeTokenizer.Token.EOF) {
            BinaryEnvelopeTokenizer.Token token = tokenizer.peekToken();

            if (token == BinaryEnvelopeTokenizer.Token.TEXT) {
                tokenizer.nextToken();
                segments.add(new StaticHexSegment(tokenizer.getValue()));
            } else if (token == BinaryEnvelopeTokenizer.Token.TAG_OPEN) {
                segments.add(parseTag(tokenizer));
            } else {
                throw new RuntimeException("Unexpected token " + token + " in " + config);
            }
        }
        return segments;
    }

    private BinarySegment parseTag(BinaryEnvelopeTokenizer t) {
        t.nextToken();
        if (t.nextToken() != BinaryEnvelopeTokenizer.Token.NAME) {
            throw new RuntimeException("Expected tag name after '{' in " + t.getConfig());
        }
        String tagName = t.getValue();
        List<String> params = new ArrayList<>();

        if (t.peekToken() == BinaryEnvelopeTokenizer.Token.PAREN_OPEN) {
            params = parseParams(t);
        }

        if (t.nextToken() != BinaryEnvelopeTokenizer.Token.TAG_CLOSE) {
            throw new RuntimeException("Expected '}' at the end of tag in " + t.getConfig());
        }

        BinarySegmentProvider provider = providerMap.get(tagName.toUpperCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown binary envelope tag: " + tagName);
        }
        return provider.create(params);
    }

    private List<String> parseParams(BinaryEnvelopeTokenizer t) {
        t.nextToken();
        List<String> params = new ArrayList<>();

        while (true) {
            if (t.nextToken() != BinaryEnvelopeTokenizer.Token.NAME) {
                throw new RuntimeException("Expected parameter name in " + t.getConfig());
            }
            params.add(t.getValue());

            BinaryEnvelopeTokenizer.Token next = t.nextToken();
            if (next == BinaryEnvelopeTokenizer.Token.PAREN_CLOSE) {
                return params;
            }
            if (next != BinaryEnvelopeTokenizer.Token.COMMA) {
                throw new RuntimeException("Expected ',' or ')' in parameters of " + t.getConfig());
            }
        }
    }
}