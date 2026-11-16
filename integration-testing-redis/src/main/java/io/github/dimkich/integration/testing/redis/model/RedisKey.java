package io.github.dimkich.integration.testing.redis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Identifies a Redis key within a logical database.
 * <p>
 * Equality is based on {@link #db} and {@link #key} only; {@link #ignored} is excluded
 * so keys can be marked to skip replication or comparison without changing identity.
 */
@Data
@EqualsAndHashCode(of = {"db", "key"})
public class RedisKey {
    private Integer db;
    private Object key;
    @JsonIgnore
    private boolean ignored;

    /**
     * Identifier of the key in the difference and in test XML.
     * <p>
     * The logical database is written as a prefix ({@code 0/user:profile:100}) only for
     * connections that track several databases; otherwise the display key is the Redis key
     * itself.
     *
     * @return display key
     */
    @JsonIgnore
    public String getDisplayKey() {
        return db == null ? String.valueOf(key) : db + "/" + key;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return db == null ? key.toString() : key.toString() + "." + db;
    }
}