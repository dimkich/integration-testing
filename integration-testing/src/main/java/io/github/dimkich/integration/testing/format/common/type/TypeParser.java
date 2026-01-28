package io.github.dimkich.integration.testing.format.common.type;

import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticGenericArrayType;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticParameterizedType;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticWildcardType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for string-based Java type descriptions used by integration-testing.
 * <p>
 * Supports:
 * <ul>
 *     <li>simple types, e.g. {@code java.lang.String}</li>
 *     <li>parameterized types, e.g. {@code java.util.List<java.lang.String>}</li>
 *     <li>wildcards with bounds, e.g. {@code ? extends Number}, {@code ? super Number}</li>
 *     <li>generic arrays, e.g. {@code java.util.List<java.lang.String>[]}</li>
 * </ul>
 * Parsed types are resolved through {@link TypeResolverFactory}.
 */
@RequiredArgsConstructor
public class TypeParser {
    private static final Type[] EMPTY_TYPE_ARRAY = new Type[]{};
    private static final Type[] OBJECT_ARRAY = new Type[]{Object.class};

    private final TypeResolverFactory typeResolverFactory;

    /**
     * Parses the given textual type description into a {@link Type}.
     *
     * @param typeDesc textual description of the type
     * @return resolved {@link Type} instance
     * @throws RuntimeException if the description has incorrect format
     */
    public Type parse(String typeDesc) {
        Tokenizer tokenizer = new Tokenizer(typeDesc);
        Type type = getType(tokenizer);
        while (tokenizer.nextToken() != Tokenizer.Token.EOF) {
        }
        return type;
    }

    /**
     * Parses the next type from the tokenizer, including optional generic
     * arguments and array dimensions.
     *
     * @param t tokenizer positioned at the beginning of the type
     * @return parsed {@link Type}
     * @throws RuntimeException if unexpected tokens are encountered
     */
    private Type getType(Tokenizer t) {
        Tokenizer.Token token = t.nextToken();
        if (token == Tokenizer.Token.QUESTION) {
            return getWildcardType(t);
        }
        if (token != Tokenizer.Token.NAME) {
            throw new RuntimeException(String.format("Expecting type name in '%s'", t.getTypeDesc()));
        }
        String name = t.getName();
        Type type = switch (t.nextToken()) {
            case EOF, COMMA, TRIANGULAR_CLOSE, SQUARE_OPEN -> typeResolverFactory.getType(name);
            case TRIANGULAR_OPEN -> new SyntheticParameterizedType(
                    typeResolverFactory.getType(name),
                    getParameterizedType(t).toArray(EMPTY_TYPE_ARRAY)
            );
            default -> throw new RuntimeException(
                    String.format("Unexpected token %s in '%s'", t.getCurrentToken(), t.getTypeDesc()));
        };
        while (t.getCurrentToken() == Tokenizer.Token.SQUARE_OPEN) {
            if (t.nextToken() != Tokenizer.Token.SQUARE_CLOSE) {
                throw new RuntimeException(String.format("Expected ']' after '[' in '%s'", t.getTypeDesc()));
            }
            type = new SyntheticGenericArrayType(type);
            t.nextToken();
        }
        return type;
    }

    /**
     * Parses a comma-separated list of type arguments until the closing
     * triangular bracket is reached.
     *
     * @param t tokenizer positioned after the opening {@code '<'}
     * @return list of parsed type arguments
     * @throws RuntimeException if the generic declaration is malformed
     */
    private List<Type> getParameterizedType(Tokenizer t) {
        List<Type> list = new ArrayList<>();
        while (true) {
            list.add(getType(t));
            if (t.getCurrentToken() == Tokenizer.Token.TRIANGULAR_CLOSE) {
                t.nextToken();
                return list;
            }
        }
    }

    /**
     * Parses a wildcard type beginning with {@code '?'}.
     * <p>
     * Supported forms:
     * <ul>
     *     <li>{@code ?}</li>
     *     <li>{@code ? extends SomeType}</li>
     *     <li>{@code ? super SomeType}</li>
     * </ul>
     *
     * @param t tokenizer positioned after the {@code '?'}
     * @return corresponding {@link SyntheticWildcardType}
     * @throws RuntimeException if the wildcard declaration is malformed
     */
    private Type getWildcardType(Tokenizer t) {
        Tokenizer.Token token = t.nextToken();
        if (token == Tokenizer.Token.NAME) {
            String bound = t.getName();
            if (t.nextToken() != Tokenizer.Token.NAME) {
                throw new RuntimeException(String.format("Wrong wildcard format in '%s'", t.getTypeDesc()));
            }
            Type type = typeResolverFactory.getType(t.getName());
            t.nextToken();
            return switch (bound) {
                case "extends" -> new SyntheticWildcardType(new Type[]{type}, EMPTY_TYPE_ARRAY);
                case "super" -> new SyntheticWildcardType(OBJECT_ARRAY, new Type[]{type});
                default -> throw new RuntimeException(String.format(
                        "Expected 'extends' or 'super' after '?' in '%s', got '%s'", t.getTypeDesc(), bound));
            };
        }
        return new SyntheticWildcardType(OBJECT_ARRAY, EMPTY_TYPE_ARRAY);
    }

    /**
     * Simple tokenizer over the textual type description.
     * <p>
     * It breaks the input into structural and name tokens and keeps track
     * of generic bracket depth to detect malformed input early.
     */
    @RequiredArgsConstructor
    static class Tokenizer {
        enum Token {NAME, TRIANGULAR_OPEN, TRIANGULAR_CLOSE, COMMA, QUESTION, SQUARE_OPEN, SQUARE_CLOSE, EOF}

        @Getter
        private final String typeDesc;
        private int index = 0;
        private int bracketDepth;
        @Getter
        private Token currentToken;
        @Getter
        private String name;

        /**
         * Reads and returns the next token from {@link #typeDesc}.
         * <p>
         * Skips leading whitespaces, keeps track of generic bracket depth
         * and validates basic structural correctness.
         *
         * @return next {@link Token}, or {@link Token#EOF} when input is exhausted
         * @throws RuntimeException if brackets are unbalanced or format is incorrect
         */
        public Token nextToken() {
            skipWhitespaces();
            if (index >= typeDesc.length()) {
                if (bracketDepth != 0) {
                    throw new RuntimeException(String.format("Incorrect format in '%s'", typeDesc));
                }
                return currentToken = Token.EOF;
            }
            return switch (typeDesc.charAt(index++)) {
                case '<' -> {
                    bracketDepth++;
                    yield currentToken = Token.TRIANGULAR_OPEN;
                }
                case '>' -> {
                    bracketDepth--;
                    if (bracketDepth < 0) {
                        throw new RuntimeException(String.format("Too much '>' in '%s'", typeDesc));
                    }
                    yield currentToken = Token.TRIANGULAR_CLOSE;
                }
                case ',' -> currentToken = Token.COMMA;
                case '?' -> currentToken = Token.QUESTION;
                case '[' -> currentToken = Token.SQUARE_OPEN;
                case ']' -> currentToken = Token.SQUARE_CLOSE;
                default -> {
                    index--;
                    yield readNameToken();
                }
            };
        }

        /**
         * Reads a contiguous sequence of non-structural characters as a {@link Token#NAME}.
         *
         * @return {@link Token#NAME}
         */
        private Token readNameToken() {
            skipWhitespaces();
            int start = index;
            while (index < typeDesc.length()) {
                char ch = typeDesc.charAt(index);
                if (ch == '<' || ch == '>' || ch == ',' || ch == '?' || ch == '[' || ch == ']'
                        || Character.isWhitespace(ch)) {
                    break;
                }
                index++;
            }
            name = typeDesc.substring(start, index);
            return currentToken = Token.NAME;
        }

        /**
         * Advances the internal cursor over any whitespace characters.
         */
        private void skipWhitespaces() {
            while (index < typeDesc.length() && Character.isWhitespace(typeDesc.charAt(index))) {
                index++;
            }
        }
    }
}
