package io.github.dimkich.integration.testing.redis.codec.segment;

import lombok.Getter;

/**
 * Tokenizes a binary format configuration string for consumption by {@link BinaryFormatParser}.
 * <p>
 * Splits the config into lexical tokens such as hex text, structural characters ({@code {}},
 * {@code ()}, {@code ,}), and tag/parameter names. Supports single- and double-quoted names
 * for parameters containing commas or other structural characters.
 *
 * @see BinaryFormatParser
 */
public class BinaryFormatTokenizer {
    /**
     * Lexical token types produced by the tokenizer.
     */
    public enum Token {
        /** Plain hex text outside of tags (e.g. {@code 524453} or {@code AA}). */
        TEXT,
        /** Opening tag delimiter <code>{</code>. */
        TAG_OPEN,
        /** Closing tag delimiter <code>}</code>. */
        TAG_CLOSE,
        /** Tag name or parameter value (identifier or quoted string). */
        NAME,
        /** Opening parenthesis <code>(</code> for tag parameters. */
        PAREN_OPEN,
        /** Closing parenthesis <code>)</code> for tag parameters. */
        PAREN_CLOSE,
        /** Comma <code>,</code> separating parameters. */
        COMMA,
        /** End of the configuration string. */
        EOF
    }

    @Getter
    private final String config;
    private int index = 0;
    /**
     * The string value of the last consumed {@link Token#TEXT} or {@link Token#NAME} token.
     * Undefined for structural tokens (e.g. TAG_OPEN, COMMA) and EOF.
     */
    @Getter
    private String value;
    private Token lookahead;

    private boolean insideTag = false;

    /**
     * Creates a tokenizer for the given configuration string.
     *
     * @param config the binary format configuration to tokenize (e.g. {@code {LEN}{CONTENT}})
     */
    public BinaryFormatTokenizer(String config) {
        this.config = config;
    }

    /**
     * Returns the next token without consuming it. Repeated calls return the same token until
     * {@link #nextToken()} is called.
     *
     * @return the next token, or {@link Token#EOF} when the config is exhausted
     */
    public Token peekToken() {
        if (lookahead == null) {
            lookahead = readNext();
        }
        return lookahead;
    }

    /**
     * Consumes and returns the next token. After this call, {@link #getValue()} holds the
     * string value for {@link Token#TEXT} or {@link Token#NAME} tokens.
     *
     * @return the next token, or {@link Token#EOF} when the config is exhausted
     */
    public Token nextToken() {
        Token res = peekToken();
        lookahead = null;
        return res;
    }

    private static boolean isStructural(char ch) {
        return switch (ch) {
            case '{', '}', '(', ')', ',', '"', '\'' -> true;
            default -> false;
        };
    }

    private Token readNext() {
        if (index >= config.length()) {
            return Token.EOF;
        }
        char ch = config.charAt(index);
        if (insideTag && (ch == '"' || ch == '\'')) {
            return readQuotedToken(ch);
        }

        if (isStructural(ch)) {
            index++;
            switch (ch) {
                case '{' -> {
                    insideTag = true;
                    return Token.TAG_OPEN;
                }
                case '}' -> {
                    insideTag = false;
                    return Token.TAG_CLOSE;
                }
                case '(' -> {
                    return Token.PAREN_OPEN;
                }
                case ')' -> {
                    return Token.PAREN_CLOSE;
                }
                case ',' -> {
                    return Token.COMMA;
                }
            }
        }
        if (!insideTag) {
            return readTextToken();
        }
        if (Character.isWhitespace(ch)) {
            index++;
            return readNext();
        }

        return readNameToken();
    }

    private Token readQuotedToken(char delimiter) {
        index++;
        StringBuilder sb = new StringBuilder();
        while (index < config.length()) {
            char ch = config.charAt(index);
            if (ch == '\\') {
                if (index + 1 < config.length()) {
                    index++;
                    sb.append(config.charAt(index));
                }
            } else if (ch == delimiter) {
                index++;
                value = sb.toString();
                return Token.NAME;
            } else {
                sb.append(ch);
            }
            index++;
        }
        throw new RuntimeException("Unclosed quote [" + delimiter + "] in " + config);
    }

    private Token readTextToken() {
        int start = index;
        while (index < config.length() && config.charAt(index) != '{') {
            index++;
        }
        value = config.substring(start, index).trim();
        return value.isEmpty() ? readNext() : Token.TEXT;
    }

    private Token readNameToken() {
        int start = index;
        while (index < config.length()) {
            char ch = config.charAt(index);
            if (isStructural(ch) || Character.isWhitespace(ch)) break;
            index++;
        }
        value = config.substring(start, index);
        return Token.NAME;
    }
}