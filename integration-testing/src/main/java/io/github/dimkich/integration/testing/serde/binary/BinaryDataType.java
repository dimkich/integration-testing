package io.github.dimkich.integration.testing.serde.binary;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Defines primitive numeric types for reading and writing binary data to a {@link ByteBuffer}.
 * <p>
 * Used by binary segment providers (e.g. {@link LengthSegmentProvider}, {@link TsSegmentProvider},
 * {@link StringSegmentProvider}) to specify the size and encoding of length, timestamp, and similar
 * numeric fields in a binary envelope layout. {@code BYTE} and {@code SHORT} are read as unsigned, so
 * length fields can use their full range (0..255 and 0..65535).
 */
@RequiredArgsConstructor
public enum BinaryDataType {
    /** 8-byte signed integer (64-bit). */
    LONG(8, (buf, val) -> buf.putLong(val.longValue()), ByteBuffer::getLong),
    /** 4-byte signed integer (32-bit). */
    INT(4, (buf, val) -> buf.putInt(val.intValue()), ByteBuffer::getInt),
    /** 2-byte integer; read as unsigned (0..65535), which suits binary length fields. */
    SHORT(2, (buf, val) -> buf.putShort(val.shortValue()), buf -> buf.getShort() & 0xFFFF),
    /** 1-byte integer; read as unsigned (0..255), which suits binary length fields. */
    BYTE(1, (buf, val) -> buf.put(val.byteValue()), buf -> buf.get() & 0xFF),
    /** 8-byte double-precision floating point (64-bit). */
    DOUBLE(8, (buf, val) -> buf.putDouble(val.doubleValue()), ByteBuffer::getDouble),
    /** 4-byte single-precision floating point (32-bit). */
    FLOAT(4, (buf, val) -> buf.putFloat(val.floatValue()), ByteBuffer::getFloat);

    /** Size in bytes of this type's representation. */
    @Getter
    private final int size;
    private final BiConsumer<ByteBuffer, Number> writer;
    private final Function<ByteBuffer, Number> reader;

    /**
     * Writes a numeric value to the buffer at its current position.
     *
     * @param buffer the buffer to write to
     * @param value  the value to write (must match this type's range)
     */
    public void write(ByteBuffer buffer, Number value) {
        writer.accept(buffer, value);
    }

    /**
     * Reads a numeric value from the buffer at its current position
     * and advances the position by this type's size.
     *
     * @param buffer the buffer to read from
     * @return the read value as a {@link Number}
     */
    public Number read(ByteBuffer buffer) {
        return reader.apply(buffer);
    }

    /**
     * Parses a type name (case-insensitive) to the corresponding enum constant.
     * Accepts "INTEGER" as an alias for "INT".
     *
     * @param type the type name (e.g. "LONG", "INT", "INTEGER", "SHORT", "BYTE", "DOUBLE", "FLOAT")
     * @return the matching {@link BinaryDataType}
     * @throws IllegalArgumentException if the type name is not recognized
     */
    public static BinaryDataType fromString(String type) {
        return switch (type.toUpperCase()) {
            case "LONG" -> LONG;
            case "INTEGER", "INT" -> INT;
            case "SHORT" -> SHORT;
            case "BYTE" -> BYTE;
            case "DOUBLE" -> DOUBLE;
            case "FLOAT" -> FLOAT;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
