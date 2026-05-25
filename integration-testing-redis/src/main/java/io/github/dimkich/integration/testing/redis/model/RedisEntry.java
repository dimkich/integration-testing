package io.github.dimkich.integration.testing.redis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.moilioncircle.redis.replicator.rdb.datatype.ExpiredType;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import io.github.dimkich.integration.testing.date.time.PeriodDuration;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Wrapper for a Redis value that may carry TTL metadata.
 * <p>
 * Used for plain string values and as the element wrapper inside {@link RedisHash}
 * fields. Expiration can be expressed as a relative {@link PeriodDuration} ({@link #ttl})
 * or an absolute {@link #expireAt} timestamp; both are kept in sync when updated.
 * <p>
 * Equality includes {@link #data} and {@link #ttl} only; {@link #expireAt} is derived.
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"data", "ttl"})
public class RedisEntry {
    @Getter
    @Setter
    private Object data;
    @Getter
    @Setter
    private PeriodDuration ttl;
    @Getter(onMethod_ = @JsonIgnore)
    private ZonedDateTime expireAt;

    /**
     * Creates an entry wrapping {@code data} with no TTL.
     *
     * @param data stored value
     */
    public RedisEntry(Object data) {
        this.data = data;
    }

    /**
     * Returns the wrapped data, instantiating {@code cls} when {@link #data} is {@code null}
     * or not of the requested type.
     *
     * @param cls expected data type
     * @param <T> data type
     * @return existing or newly created data instance
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T getOrCreateData(Class<T> cls) {
        return (T) (data = data == null || data.getClass() != cls ? cls.getConstructor().newInstance() : data);
    }

    /**
     * Sets expiration from an absolute epoch-millis timestamp.
     *
     * @param now    reference time used to compute {@link #ttl}
     * @param millis expiration instant in epoch milliseconds, or {@code null} to skip
     */
    public void setExpireAt(ZonedDateTime now, Long millis) {
        if (millis == null) {
            return;
        }
        setExpireAt(now, Instant.ofEpochMilli(millis).atZone(now.getZone()));
    }

    /**
     * Sets expiration from an RDB {@link KeyValuePair} expiration field.
     *
     * @param now   reference time used to compute {@link #ttl}
     * @param event replication event carrying expiration type and value
     */
    public void setExpireAt(ZonedDateTime now, KeyValuePair<?, ?> event) {
        if (event.getExpiredType() == ExpiredType.NONE) {
            return;
        }
        long val = event.getExpiredValue();
        if (event.getExpiredType() == ExpiredType.SECOND) {
            val *= 1000L;
        }
        setExpireAt(now, Instant.ofEpochMilli(val).atZone(now.getZone()));
    }

    /**
     * Sets or clears the absolute expiration instant and recomputes {@link #ttl}.
     *
     * @param now      reference time used to compute {@link #ttl}
     * @param expireAt expiration instant, or {@code null} to clear TTL metadata
     */
    public void setExpireAt(ZonedDateTime now, ZonedDateTime expireAt) {
        if (expireAt == null) {
            this.expireAt = null;
            this.ttl = null;
            return;
        }
        this.expireAt = expireAt;
        ttl = PeriodDuration.between(now, expireAt);
    }

    /**
     * Refreshes {@link #ttl} relative to {@code now} and reports whether the entry has expired.
     *
     * @param now current time
     * @return {@code true} when {@link #expireAt} is set and TTL is zero or negative
     */
    public boolean setNow(ZonedDateTime now) {
        if (expireAt == null) {
            return false;
        }
        ttl = PeriodDuration.between(now, expireAt);
        return ttl.isNegative() || ttl.isZero();
    }

    /**
     * Returns the absolute expiration instant derived from {@link #expireAt} or {@link #ttl}.
     *
     * @param now reference time used when {@link #ttl} is relative
     * @return expiration instant, or {@code null} when no TTL is configured
     */
    public Instant getExpireInstant(ZonedDateTime now) {
        if (expireAt != null) {
            return expireAt.toInstant();
        }
        if (ttl != null) {
            return now.plus(ttl.getPeriod())
                    .plus(ttl.getDuration())
                    .toInstant();
        }
        return null;
    }

    /**
     * Deserializes and replaces {@link #data} using the schema's value codec.
     *
     * @param schema codecs for the stored value
     * @param data   raw value bytes
     */
    public void setValue(RedisDataSchema schema, byte[] data) {
        this.data = schema.getValueCodec().deserialize(data);
    }

    /**
     * Adds {@code delta} to the numeric {@link #data}, storing the result as a plain string.
     *
     * @param delta increment amount, ignored when {@code null}
     * @throws RuntimeException when existing {@link #data} is not numeric
     */
    public void increment(Number delta) {
        if (delta == null) {
            return;
        }
        BigDecimal current = BigDecimal.ZERO;
        if (data != null) {
            try {
                current = new BigDecimal(data.toString());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Value is not a number: " + data);
            }
        }
        BigDecimal result = current.add(new BigDecimal(delta.toString()));
        this.data = result.stripTrailingZeros().toPlainString();
    }

    /**
     * Returns {@code true} when {@link #data} is {@code null} or an empty {@link RedisValue}.
     *
     * @return whether the entry has no meaningful content
     */
    @JsonIgnore
    public boolean isEmpty() {
        if (data instanceof RedisValue redisValue) {
            return redisValue.isEmpty();
        }
        return data == null;
    }
}
