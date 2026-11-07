package io.github.dimkich.integration.testing.postgresql.model;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Lazily built positional layout of a PostgreSQL {@code record_out} tuple.
 * <p>
 * The layout records only where each field starts and ends. A field value is materialized as a
 * {@link ByteBuffer} on demand; quoted values without escapes remain views into the source, while
 * escaped values are decoded once while the layout is parsed. The layout can be extended for a
 * prefix (for example the primary key) and completed later when a full row is needed. The source
 * buffer is read from index zero; callers always pass a wrapped array or a field slice, so its
 * position is zero and its limit is the end of the record.
 */
public final class PostgresTupleLayout {
    private static final byte NULL = 0;
    private static final byte VALUE = 1;

    private final ByteBuffer source;
    private final int limit;
    private final int expectedFields;
    private final String location;

    private int[] starts = new int[0];
    private int[] ends = new int[0];
    private byte[] flags = new byte[0];
    private Map<Integer, byte[]> decodedValues;
    private int parsedFields;
    private int cursor;
    private boolean started;
    private boolean complete;

    public PostgresTupleLayout(ByteBuffer source, int expectedFields, String location) {
        if (source == null) {
            throw new IllegalArgumentException("PostgreSQL record source must not be null");
        }
        if (expectedFields < 0) {
            throw new IllegalArgumentException("Expected field count must not be negative");
        }
        if (source.position() != 0) {
            throw new IllegalArgumentException("PostgreSQL record source must start at position 0");
        }
        this.source = source.duplicate();
        this.limit = this.source.limit();
        this.expectedFields = expectedFields;
        this.location = location;
    }

    public void ensureFields(int count) {
        if (count < 0 || count > expectedFields) {
            throw new IllegalArgumentException("Field count " + count + " is out of range");
        }
        if (count <= parsedFields) {
            if (count == expectedFields && !complete) {
                complete();
            }
            return;
        }
        startRecord();
        for (int field = parsedFields; field < count; field++) {
            if (field > 0) {
                expectComma(field);
            }
            parseField(field);
            parsedFields++;
        }
        if (count == expectedFields) {
            complete();
        }
    }

    public ByteBuffer getField(int index) {
        if (index < 0 || index >= expectedFields) {
            throw new IllegalArgumentException("Field index " + index + " is out of range");
        }
        ensureFields(index + 1);
        return materialize(index);
    }

    public ByteBuffer[] getFields() {
        ensureFields(expectedFields);
        ByteBuffer[] result = new ByteBuffer[expectedFields];
        for (int i = 0; i < expectedFields; i++) {
            result[i] = materialize(i);
        }
        return result;
    }

    private void startRecord() {
        if (started) {
            return;
        }
        if (limit == 0 || get(0) != '(') {
            throw error("record must start with '('");
        }
        cursor = 1;
        started = true;
    }

    private void complete() {
        startRecord();
        if (cursor >= limit) {
            throw error("unexpected end of record");
        }
        if (get(cursor) != ')') {
            throw error("expected ')' after field " + (expectedFields - 1));
        }
        cursor++;
        if (cursor != limit) {
            throw error("unexpected trailing data");
        }
        complete = true;
    }

    private void parseField(int field) {
        if (cursor >= limit) {
            throw error("unexpected end of record");
        }
        byte current = get(cursor);
        if (current == ',' || current == ')') {
            putNull(field);
            return;
        }
        if (current == '"') {
            parseQuoted(field);
            return;
        }
        parseUnquoted(field);
    }

    private void parseQuoted(int field) {
        int start = cursor + 1;
        int position = start;
        ByteAccumulator decoded = null;
        while (position < limit) {
            byte current = get(position);
            if (current == '"') {
                if (position + 1 < limit && get(position + 1) == '"') {
                    if (decoded == null) {
                        decoded = new ByteAccumulator();
                        decoded.append(source, start, position);
                    }
                    decoded.append((byte) '"');
                    position += 2;
                    continue;
                }
                int next = position + 1;
                if (next < limit) {
                    byte delimiter = get(next);
                    if (delimiter != ',' && delimiter != ')') {
                        throw error("expected ',' or ')' after quoted field " + field);
                    }
                }
                if (decoded == null) {
                    put(field, start, position);
                } else {
                    putDecoded(field, decoded.toByteArray());
                }
                cursor = next;
                return;
            }
            if (current == '\\') {
                if (position + 1 >= limit) {
                    throw error("dangling backslash in quoted field " + field);
                }
                if (decoded == null) {
                    decoded = new ByteAccumulator();
                    decoded.append(source, start, position);
                }
                decoded.append(get(position + 1));
                position += 2;
                continue;
            }
            if (decoded != null) {
                decoded.append(current);
            }
            position++;
        }
        throw error("unterminated field " + field);
    }

    private void parseUnquoted(int field) {
        int start = cursor;
        int position = start;
        while (position < limit) {
            byte current = get(position);
            if (current == ',' || current == ')') {
                put(field, start, position);
                cursor = position;
                return;
            }
            if (current == '"') {
                throw error("unexpected '\"' in unquoted field " + field);
            }
            position++;
        }
        throw error("unterminated field " + field);
    }

    private void expectComma(int field) {
        if (cursor >= limit) {
            throw error("unexpected end of record");
        }
        if (get(cursor) != ',') {
            throw error("expected ',' before field " + field);
        }
        cursor++;
    }

    private ByteBuffer materialize(int field) {
        byte[] decoded = decodedValues == null ? null : decodedValues.get(field);
        if (decoded != null) {
            return ByteBuffer.wrap(decoded);
        }
        if (flags[field] == NULL) {
            return null;
        }
        int start = starts[field];
        int end = ends[field];
        return source.slice(start, end - start);
    }

    private void put(int field, int start, int end) {
        ensureCapacity(field + 1);
        starts[field] = start;
        ends[field] = end;
        flags[field] = VALUE;
    }

    private void putNull(int field) {
        ensureCapacity(field + 1);
        starts[field] = -1;
        ends[field] = -1;
        flags[field] = NULL;
    }

    private void putDecoded(int field, byte[] value) {
        put(field, -1, -1);
        if (decodedValues == null) {
            decodedValues = new HashMap<>();
        }
        decodedValues.put(field, value);
    }

    private void ensureCapacity(int size) {
        if (starts.length >= size) {
            return;
        }
        int newSize = starts.length == 0
                ? Math.max(size, Math.min(expectedFields, 16))
                : Math.max(size, starts.length * 2);
        starts = Arrays.copyOf(starts, newSize);
        ends = Arrays.copyOf(ends, newSize);
        flags = Arrays.copyOf(flags, newSize);
    }

    private byte get(int index) {
        if (index < 0 || index >= limit) {
            throw error("unexpected end of record");
        }
        return source.get(index);
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(
                "Malformed PostgreSQL record for " + location + ": " + message);
    }

    private static final class ByteAccumulator {
        private byte[] bytes = new byte[16];
        private int size;

        private void append(byte value) {
            ensure(1);
            bytes[size++] = value;
        }

        private void append(ByteBuffer source, int start, int end) {
            int length = end - start;
            ensure(length);
            source.get(start, bytes, size, length);
            size += length;
        }

        private byte[] toByteArray() {
            return size == bytes.length ? bytes : Arrays.copyOf(bytes, size);
        }

        private void ensure(int extra) {
            if (size + extra <= bytes.length) {
                return;
            }
            int capacity = Math.max(size + extra, bytes.length * 2);
            bytes = Arrays.copyOf(bytes, capacity);
        }
    }
}
