package io.github.dimkich.integration.testing.redis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * Score–member pair stored in a {@link RedisZSet}.
 * <p>
 * Ordering is by ascending {@link #score}; equal scores are tie-broken by
 * {@link #member} using {@link RedisDataComparator}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisZSetEntry implements Comparable<RedisZSetEntry> {
    private final static RedisDataComparator memberComparator = new RedisDataComparator();

    private Object member;
    private BigDecimal score;

    /** {@inheritDoc} */
    @Override
    public int compareTo(@NotNull RedisZSetEntry o) {
        int res = getScore().compareTo(o.getScore());
        return res != 0 ? res : memberComparator.compare(getMember(), o.getMember());
    }
}
