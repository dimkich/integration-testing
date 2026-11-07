package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTypeDescriptor;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a PostgreSQL array literal into nested {@link List}s. Elements are converted by the
 * converter of the element type; quoted and escaped values, {@code NULL} elements, and
 * multidimensional arrays are supported. A non-default lower bound is skipped.
 */
final class PostgresArrayConverter extends PostgresConverter {

    private final PostgresTypeDescriptor type;
    private final TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> elementConverter;

    PostgresArrayConverter(ObjectProvider<SerdeManager> serdeManager, PostgresTypeDescriptor type) {
        this.type = type;
        PostgresTypeDescriptor elementType = type.getElement() == null
                ? new PostgresTypeDescriptor("text", 'b', null, List.of(), ',') : type.getElement();
        this.elementConverter = serdeManager.getObject().resolve(TestSerdeProperties.Default.INSTANCE,
                ByteBuffer.class, Object.class, PostgresColumnSerdeContext.class, elementType);
    }

    @Override
    public Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        if (input == null) {
            return null;
        }
        return new Parser(decode(input), type, elementConverter, context).parse();
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Parser {
        private final String text;
        private final PostgresTypeDescriptor type;
        private final TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> elementConverter;
        private final PostgresColumnSerdeContext context;
        private int pos;

        Object parse() {
            skipBounds();
            return parseList();
        }

        private void skipBounds() {
            if (pos < text.length() && text.charAt(pos) == '[') {
                int equals = text.indexOf('=', pos);
                int brace = text.indexOf('{', pos);
                if (equals >= 0 && brace >= 0 && equals < brace) {
                    pos = equals + 1;
                }
            }
        }

        private List<Object> parseList() {
            expect('{');
            List<Object> values = new ArrayList<>();
            if (peek('}')) {
                pos++;
                return values;
            }
            while (true) {
                if (peek('{')) {
                    values.add(parseList());
                } else {
                    values.add(parseElement());
                }
                if (peek(type.getDelimiter())) {
                    pos++;
                    continue;
                }
                expect('}');
                return values;
            }
        }

        private Object parseElement() {
            String value;
            boolean quoted = false;
            if (peek('"')) {
                quoted = true;
                StringBuilder builder = new StringBuilder();
                boolean closed = false;
                pos++;
                while (pos < text.length()) {
                    char ch = text.charAt(pos);
                    if (ch == '\\' && pos + 1 < text.length()) {
                        builder.append(text.charAt(pos + 1));
                        pos += 2;
                        continue;
                    }
                    if (ch == '"') {
                        pos++;
                        closed = true;
                        break;
                    }
                    builder.append(ch);
                    pos++;
                }
                if (!closed) {
                    throw new IllegalArgumentException(
                            "Unterminated quoted PostgreSQL array element at index " + pos);
                }
                value = builder.toString();
            } else {
                int start = pos;
                while (pos < text.length() && text.charAt(pos) != type.getDelimiter()
                        && text.charAt(pos) != '}') {
                    pos++;
                }
                value = text.substring(start, pos).trim();
            }
            if (!quoted && "NULL".equals(value)) {
                return null;
            }
            ByteBuffer bytes = ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
            PostgresColumnSerdeContext elementContext = context.getElementContext();
            return elementConverter.convert(bytes, elementContext);
        }

        private boolean peek(char ch) {
            return pos < text.length() && text.charAt(pos) == ch;
        }

        private void expect(char ch) {
            if (!peek(ch)) {
                throw new IllegalArgumentException("Malformed PostgreSQL array literal at index " + pos
                        + ", expected '" + ch + "'");
            }
            pos++;
        }
    }
}
