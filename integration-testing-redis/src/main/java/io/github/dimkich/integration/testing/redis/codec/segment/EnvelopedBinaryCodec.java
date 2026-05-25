package io.github.dimkich.integration.testing.redis.codec.segment;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A {@link RedisDataCodec} that wraps payload data in a binary envelope defined by a sequence of
 * {@link BinarySegment}s.
 * <p>
 * The envelope layout consists of header segments, exactly one {@link ContentSegmentProvider.ContentSegment}
 * for the payload, and footer segments. On serialization, the delegate codec produces the payload; header
 * and footer segments (e.g. version, length prefix, checksum) are written around it. On deserialization,
 * segments are read in order to extract the payload, which is then passed to the delegate for decoding.
 * <p>
 * Typical use: parse a format string (e.g. {@code {VER(INT, BE)}{LEN(INT, BE)}{CONTENT}{CRC32}}) via
 * {@link BinaryFormatParser} to obtain a {@code List<BinarySegment>}, then wrap any existing
 * {@link RedisDataCodec} with this envelope.
 *
 * @see BinaryFormatParser
 * @see BinarySegment
 * @see ContentSegmentProvider.ContentSegment
 */
public class EnvelopedBinaryCodec implements RedisDataCodec {
    private final RedisDataCodec delegate;
    private final List<BinarySegment> segments;
    private final int headerSize;
    private final int footerSize;

    /**
     * Creates a codec that wraps the delegate's payload in the given binary envelope.
     * The segments must contain exactly one {@link ContentSegmentProvider.ContentSegment}; header
     * size is the sum of fixed lengths of segments before it, footer size the sum after.
     *
     * @param delegate the underlying codec used to serialize/deserialize the payload
     * @param segments the envelope layout; must contain exactly one content segment
     * @throws IllegalArgumentException if there are multiple or no content segments
     */
    public EnvelopedBinaryCodec(RedisDataCodec delegate, List<BinarySegment> segments) {
        this.delegate = delegate;
        this.segments = segments;

        int contentIndex = -1;
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i) instanceof ContentSegmentProvider.ContentSegment) {
                if (contentIndex != -1) {
                    throw new IllegalArgumentException("Multiple {CONTENT} tags");
                }
                contentIndex = i;
            }
        }
        if (contentIndex == -1) {
            throw new IllegalArgumentException("Missing {CONTENT} tag");
        }

        this.headerSize = segments.subList(0, contentIndex).stream()
                .mapToInt(BinarySegment::length).sum();
        this.footerSize = segments.subList(contentIndex + 1, segments.size()).stream()
                .mapToInt(BinarySegment::length).sum();
    }

    /**
     * Serializes the object using the delegate codec and wraps the result in the binary envelope.
     * Header segments, content, and footer segments are written in order.
     *
     * @param object the object to serialize
     * @return the enveloped bytes, or {@code null} if the delegate returns null
     */
    @Override
    public byte[] serialize(Object object) {
        byte[] payload = delegate.serialize(object);
        if (payload == null) return null;

        ByteBuffer buffer = ByteBuffer.allocate(headerSize + payload.length + footerSize);
        for (BinarySegment segment : segments) {
            segment.write(buffer, payload);
        }
        return buffer.array();
    }

    /**
     * Deserializes enveloped bytes by reading segments in order to extract the payload, then
     * delegating to the underlying codec for decoding.
     *
     * @param data the enveloped bytes; may be {@code null}
     * @return the decoded object, or {@code null} if {@code data} is null or the extracted payload is null
     */
    @Override
    public Object deserialize(byte[] data) {
        if (data == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        ReadContext ctx = new ReadContext();
        ctx.setSuffixSize(footerSize);

        for (BinarySegment segment : segments) {
            segment.read(buffer, ctx);
        }

        return delegate.deserialize(ctx.getPayload());
    }
}