package io.github.dimkich.integration.testing.format.common.type;

import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticGenericArrayType;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticParameterizedType;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticWildcardType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TypeParser {
    private static final Type[] EMPTY_TYPE_ARRAY = new Type[]{};
    private static final Type[] OBJECT_ARRAY = new Type[]{Object.class};

    private final TypeResolverFactory typeResolverFactory;

    public Type parse(String typeDesc) {
        Tokenizer tokenizer = new Tokenizer(typeDesc);
        Type type = getType(tokenizer);
        while (tokenizer.nextToken() != Tokenizer.Token.EOF) {
        }
        return type;
    }

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

        private void skipWhitespaces() {
            while (index < typeDesc.length() && Character.isWhitespace(typeDesc.charAt(index))) {
                index++;
            }
        }
    }
}
