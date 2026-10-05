package io.github.dimkich.integration.testing.serde.binary;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Applies a parsed binary envelope layout ({@link BinaryEnvelopeParser}) to payload bytes: {@link #wrap}
 * writes header and footer segments around the payload, {@link #unwrap} reads them back and extracts
 * the payload. The layout must contain exactly one {@code {CONTENT}} segment.
 *
 * @see BinaryEnvelopeParser
 * @see BinarySegment
 */
public class BinaryEnvelope {

    private final List<BinarySegment> segments;
    private final int headerSize;
    private final int footerSize;

    public BinaryEnvelope(List<BinarySegment> segments) {
        this.segments = segments;
        int contentIndex = contentIndex(segments);
        this.headerSize = segments.subList(0, contentIndex).stream().mapToInt(BinarySegment::length).sum();
        this.footerSize = segments.subList(contentIndex + 1, segments.size()).stream()
                .mapToInt(BinarySegment::length).sum();
    }

    public byte[] wrap(byte[] payload) {
        if (payload == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(headerSize + payload.length + footerSize);
        for (BinarySegment segment : segments) {
            segment.write(buffer, payload);
        }
        return buffer.array();
    }

    public byte[] unwrap(byte[] input) {
        if (input == null) {
            return null;
        }
        ReadContext readContext = new ReadContext();
        readContext.setSuffixSize(footerSize);
        ByteBuffer buffer = ByteBuffer.wrap(input);
        for (BinarySegment segment : segments) {
            segment.read(buffer, readContext);
        }
        return readContext.getPayload();
    }

    private static int contentIndex(List<BinarySegment> segments) {
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
        return contentIndex;
    }
}
