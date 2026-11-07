package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import lombok.Value;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts a PostgreSQL {@code hstore} literal into an insertion-ordered map. Quoted and escaped
 * tokens are supported, and unquoted {@code NULL} becomes a {@code null} map value.
 */
final class PostgresHstoreConverter extends PostgresConverter {

    @Override
    public Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        if (input == null) {
            return null;
        }
        return parseHstore(decode(input));
    }

    private static Map<String, Object> parseHstore(String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        int pos = 0;
        while (pos < text.length()) {
            while (pos < text.length() && (Character.isWhitespace(text.charAt(pos)) || text.charAt(pos) == ',')) {
                pos++;
            }
            if (pos >= text.length()) {
                break;
            }
            Token key = readToken(text, pos, true);
            pos = key.getNext();
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
            if (pos + 1 >= text.length() || text.charAt(pos) != '=' || text.charAt(pos + 1) != '>') {
                throw new IllegalArgumentException("Malformed hstore literal near index " + pos);
            }
            pos += 2;
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
            Token value = readToken(text, pos, false);
            pos = value.getNext();
            result.put(key.getValue(), value.isNull() ? null : value.getValue());
        }
        return result;
    }

    private static Token readToken(String text, int start, boolean key) {
        if (start < text.length() && text.charAt(start) == '"') {
            StringBuilder builder = new StringBuilder();
            int pos = start + 1;
            while (pos < text.length()) {
                char ch = text.charAt(pos);
                if (ch == '\\' && pos + 1 < text.length()) {
                    builder.append(text.charAt(pos + 1));
                    pos += 2;
                    continue;
                }
                if (ch == '"') {
                    return new Token(builder.toString(), pos + 1, false);
                }
                builder.append(ch);
                pos++;
            }
            throw new IllegalArgumentException("Unterminated hstore quoted token");
        }
        int pos = start;
        while (pos < text.length() && text.charAt(pos) != ','
                && !(key && pos + 1 < text.length() && text.charAt(pos) == '=' && text.charAt(pos + 1) == '>')) {
            pos++;
        }
        String value = text.substring(start, pos).trim();
        return new Token(value, pos, !key && "NULL".equals(value));
    }

    @Value
    private static class Token {
        String value;
        int next;
        boolean isNull;
    }
}
