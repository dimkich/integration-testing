package io.github.dimkich.integration.testing.redis.codec.segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Parses a binary format configuration string into a sequence of {@link BinarySegment}s.
 * <p>
 * The format supports:
 * <ul>
 *   <li>Plain hex text — literal bytes, e.g. {@code 524453} or {@code AA}</li>
 *   <li>Named tags in curly braces — e.g. {@code {LEN}}, {@code {VER(5)}}, {@code {STR(io, Integer, BE)}}</li>
 * </ul>
 * Tags are looked up in the configured {@link BinarySegmentProvider}s by name (case-insensitive).
 * Tag parameters in parentheses are passed to the provider's {@link BinarySegmentProvider#create(List)}.
 * Parameters may be quoted with {@code "} or {@code '} to include commas and structural chars.
 * <p>
 * Parsed layouts are cached per configuration string for reuse.
 * <p>
 * <b>Examples</b> 
 * <pre>
 * {LEN}{CONTENT}                              // length prefix + content, default INT/BE
 * {FIX(8, 0)}{LEN(Long, LE)}{CONTENT}         // Redisson RMapCache layout
 * {VER(1)}{TS(LONG, BE)}{LEN(INT, BE)}{CONTENT}{CRC32(BE)}  // custom protocol with checksum
 * AA{STR(io, Integer, BIG_ENDIAN)}{CONTENT}   // hex prefix + string length + content
 * AA{STR(12)}{CONTENT}{FIX(2)}                // fixed string "12", content, 2-byte suffix
 * {VER(5)}{CONTENT}                           // version byte + content
 * 00{CONTENT}                                 // null marker + content
 * {STR("Data, Part 1")}{CONTENT}              // quoted param with comma
 * {STR('Simple Text')}{CONTENT}               // single-quoted param
 * {STR("He said \"Hi\"")}{CONTENT}            // escaped quotes in double-quoted param
 * {STR("Prefix, Value", INT, LITTLE_ENDIAN)}{CONTENT}  // multiple params, comma in quoted first
 * </pre>
 */
public class BinaryFormatParser {
    private final Map<String, BinarySegmentProvider> providerMap;
    private final Map<String, List<BinarySegment>> layoutCache = new ConcurrentHashMap<>();

    /**
     * Creates a parser with the given segment providers.
     *
     * @param providers list of providers; each tag name must be unique (case-insensitive)
     * @throws IllegalStateException if two providers share the same tag name
     */
    public BinaryFormatParser(List<BinarySegmentProvider> providers) {
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
     * Parses the binary format configuration into a list of segments.
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
        BinaryFormatTokenizer tokenizer = new BinaryFormatTokenizer(config);
        List<BinarySegment> segments = new ArrayList<>();

        while (tokenizer.peekToken() != BinaryFormatTokenizer.Token.EOF) {
            BinaryFormatTokenizer.Token token = tokenizer.peekToken();

            if (token == BinaryFormatTokenizer.Token.TEXT) {
                tokenizer.nextToken();
                segments.add(new StaticHexSegment(tokenizer.getValue()));
            } else if (token == BinaryFormatTokenizer.Token.TAG_OPEN) {
                segments.add(parseTag(tokenizer));
            } else {
                throw new RuntimeException("Unexpected token " + token + " in " + config);
            }
        }
        return segments;
    }

    private BinarySegment parseTag(BinaryFormatTokenizer t) {
        t.nextToken();
        if (t.nextToken() != BinaryFormatTokenizer.Token.NAME) {
            throw new RuntimeException("Expected tag name after '{' in " + t.getConfig());
        }
        String tagName = t.getValue();
        List<String> params = new ArrayList<>();

        if (t.peekToken() == BinaryFormatTokenizer.Token.PAREN_OPEN) {
            params = parseParams(t);
        }

        if (t.nextToken() != BinaryFormatTokenizer.Token.TAG_CLOSE) {
            throw new RuntimeException("Expected '}' at the end of tag in " + t.getConfig());
        }

        BinarySegmentProvider provider = providerMap.get(tagName.toUpperCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown binary format tag: " + tagName);
        }
        return provider.create(params);
    }

    private List<String> parseParams(BinaryFormatTokenizer t) {
        t.nextToken();
        List<String> params = new ArrayList<>();

        while (true) {
            if (t.nextToken() != BinaryFormatTokenizer.Token.NAME) {
                throw new RuntimeException("Expected parameter name in " + t.getConfig());
            }
            params.add(t.getValue());

            BinaryFormatTokenizer.Token next = t.nextToken();
            if (next == BinaryFormatTokenizer.Token.PAREN_CLOSE) {
                return params;
            }
            if (next != BinaryFormatTokenizer.Token.COMMA) {
                throw new RuntimeException("Expected ',' or ')' in parameters of " + t.getConfig());
            }
        }
    }
}