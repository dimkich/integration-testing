package io.github.dimkich.integration.testing.wait.completion.parser;

import lombok.Getter;

/**
 * Tokenizer for the Byte Buddy selector language used by wait-completion.
 * <p>
 * It converts a selector string into a stream of {@link Token} values and keeps
 * track of the current token, its lexeme and the current position in the input.
 * The tokenizer is intentionally simple and only understands the minimal set of
 * constructs required by {@code ByteBuddySelectorParser}.
 */
public class ByteBuddySelectorTokenizer {

    /**
     * Tokens that can be produced by {@link ByteBuddySelectorTokenizer}.
     */
    public enum Token {
        SELECT,
        IDENTIFIER,
        LPAREN, RPAREN,
        HASH,
        PLUS,
        AT,
        COMMA,
        DOTDOT,
        AND,
        EOF
    }

    private final String input;
    @Getter
    private int pos = 0;
    @Getter
    private Token currentToken = null;
    @Getter
    private String lexeme;

    /**
     * Keyword that starts a class selector.
     */
    private static final String SELECT_CLASS = "class";
    /**
     * Keyword that starts a method selector.
     */
    private static final String SELECT_METHOD = "method";
    /**
     * Lexeme used for logical AND between selectors.
     */
    private static final String AND_OP = "&&";

    /**
     * Creates a new tokenizer for the given selector string.
     *
     * @param input selector expression to tokenize; must not be {@code null}
     * @throws IllegalArgumentException if {@code input} is {@code null}
     */
    public ByteBuddySelectorTokenizer(String input) {
        if (input == null) throw new IllegalArgumentException("Input must not be null");
        this.input = input;
    }

    /**
     * Advances to the next token in the input and returns it.
     * <p>
     * When the end of input is reached this method returns {@link Token#EOF}
     * and keeps returning it on subsequent calls.
     *
     * @return the next {@link Token} in the input
     * @throws IllegalArgumentException if an unexpected character is encountered
     */
    public Token nextToken() {
        skipWhitespace();
        if (pos >= input.length()) {
            currentToken = Token.EOF;
            lexeme = "";
            return currentToken;
        }

        char c = input.charAt(pos);
        if (pos + 1 < input.length() && c == '&' && input.charAt(pos + 1) == '&') {
            currentToken = Token.AND;
            lexeme = AND_OP;
            pos += 2;
            return currentToken;
        }

        if (pos + 1 < input.length() && c == '.' && input.charAt(pos + 1) == '.') {
            currentToken = Token.DOTDOT;
            lexeme = "..";
            pos += 2;
            return currentToken;
        }

        if (c == '#') {
            currentToken = Token.HASH;
            lexeme = "#";
            pos++;
        } else if (c == '+') {
            currentToken = Token.PLUS;
            lexeme = "+";
            pos++;
        } else if (c == '@') {
            currentToken = Token.AT;
            lexeme = "@";
            pos++;
        } else if (c == '(') {
            currentToken = Token.LPAREN;
            lexeme = "(";
            pos++;
        } else if (c == ')') {
            currentToken = Token.RPAREN;
            lexeme = ")";
            pos++;
        } else if (c == ',') {
            currentToken = Token.COMMA;
            lexeme = ",";
            pos++;
        } else if (Character.isJavaIdentifierStart(c) || c == '$') {
            int start = pos;
            while (pos < input.length()) {
                char ch = input.charAt(pos);
                if (Character.isJavaIdentifierPart(ch) || ch == '$' || ch == '.') {
                    pos++;
                } else {
                    break;
                }
            }
            lexeme = input.substring(start, pos);
            if ((currentToken == null || currentToken == Token.AND)
                    && (SELECT_CLASS.equals(lexeme) || SELECT_METHOD.equals(lexeme))) {
                currentToken = Token.SELECT;
            } else {
                currentToken = Token.IDENTIFIER;
            }
        } else {
            throw new IllegalArgumentException("Unexpected character at position " + pos + ": '" + c + "'");
        }

        return currentToken;
    }

    /**
     * Skips any whitespace characters starting at the current position.
     */
    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }
}