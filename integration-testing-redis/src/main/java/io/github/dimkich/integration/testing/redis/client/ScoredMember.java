package io.github.dimkich.integration.testing.redis.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Member-score pair of a {@code ZADD} command.
 * <p>
 * A plain class rather than a record: {@link #getMember()} exposes raw bytes, for which record
 * equality and hash codes would compare references instead of content.
 */
@Getter
@RequiredArgsConstructor
public final class ScoredMember {
    private final byte[] member;
    private final double score;
}
