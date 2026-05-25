package io.github.dimkich.integration.testing.redis.model;

import com.moilioncircle.redis.replicator.rdb.datatype.ZSetEntry;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * In-memory representation of a Redis sorted set (ZSET).
 * <p>
 * Entries are ordered by {@link RedisZSetEntry#compareTo(RedisZSetEntry)} in a
 * {@link TreeSet}, with a parallel {@link #dict} index for O(1) lookup and update by
 * member. Score changes remove and re-insert the entry to maintain sort order.
 */
public class RedisZSet extends TreeSet<RedisZSetEntry> implements RedisValue {
    private final Map<Object, RedisZSetEntry> dict = new HashMap<>();

    /**
     * Inserts or updates a member decoded from an RDB ZSET entry.
     *
     * @param schema codecs for member values
     * @param entry  raw ZSET entry from the replicator
     */
    public void putEntry(RedisDataSchema schema, ZSetEntry entry) {
        Object member = schema.getValueCodec().deserialize(entry.getElement());
        this.put(member, BigDecimal.valueOf(entry.getScore()));
    }

    /**
     * Inserts or updates a member with the given score.
     * <p>
     * No-op when the member already exists with the same score.
     *
     * @param member sorted-set member
     * @param score  member score
     */
    public void put(Object member, BigDecimal score) {
        RedisZSetEntry oldEntry = dict.get(member);
        if (oldEntry != null) {
            if (oldEntry.getScore().compareTo(score) == 0) {
                return;
            }
            super.remove(oldEntry);
        }

        RedisZSetEntry newEntry = new RedisZSetEntry(member, score);
        dict.put(member, newEntry);
        super.add(newEntry);
    }

    /**
     * Removes a member by value, updating both the sorted set and the member index.
     *
     * @param member member to remove
     */
    public void removeMember(Object member) {
        RedisZSetEntry entry = dict.remove(member);
        if (entry != null) {
            super.remove(entry);
        }
    }

    /**
     * Returns the score of {@code member}, or {@code null} if the member is absent.
     *
     * @param member sorted-set member
     * @return member score, or {@code null}
     */
    public BigDecimal getScore(Object member) {
        RedisZSetEntry entry = dict.get(member);
        return entry != null ? entry.getScore() : null;
    }

    /** {@inheritDoc} */
    @Override
    public RedisZSetEntry pollFirst() {
        RedisZSetEntry entry = super.pollFirst();
        if (entry != null) {
            dict.remove(entry.getMember());
        }
        return entry;
    }

    /** {@inheritDoc} */
    @Override
    public RedisZSetEntry pollLast() {
        RedisZSetEntry entry = super.pollLast();
        if (entry != null) {
            dict.remove(entry.getMember());
        }
        return entry;
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(RedisZSetEntry entry) {
        put(entry.getMember(), entry.getScore());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Object o) {
        if (o instanceof RedisZSetEntry) {
            removeMember(((RedisZSetEntry) o).getMember());
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        dict.clear();
        super.clear();
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public Iterator<RedisZSetEntry> iterator() {
        Iterator<RedisZSetEntry> it = super.iterator();
        return new Iterator<>() {
            private RedisZSetEntry lastReturned;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public RedisZSetEntry next() {
                return lastReturned = it.next();
            }

            @Override
            public void remove() {
                it.remove();
                if (lastReturned != null) {
                    dict.remove(lastReturned.getMember());
                }
            }
        };
    }
}