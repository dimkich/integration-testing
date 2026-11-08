package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis rank range normalized against the current collection size.
 * <p>
 * Negative indexes count from the end ({@code -1} is the last element) and out-of-bounds values
 * are clamped, matching the semantics of {@code LTRIM}, {@code ZREMRANGEBYRANK} and other
 * rank-based Redis commands.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RankRange {
    private final int start;
    private final int stop;

    /**
     * Normalizes a raw Redis rank range.
     *
     * @param size number of elements in the collection
     * @param from start rank sent by the client; negative values count from the end
     * @param to   stop rank sent by the client; negative values count from the end
     * @return normalized inclusive range, or {@code null} when it selects no elements
     */
    public static RankRange of(int size, long from, long to) {
        long start = from < 0 ? size + from : from;
        long stop = to < 0 ? size + to : to;
        if (start < 0) {
            start = 0;
        }
        if (stop >= size) {
            stop = size - 1;
        }
        if (start >= size || start > stop) {
            return null;
        }
        return new RankRange((int) start, (int) stop);
    }
}
