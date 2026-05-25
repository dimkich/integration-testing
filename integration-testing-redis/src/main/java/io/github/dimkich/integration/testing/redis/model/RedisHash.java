package io.github.dimkich.integration.testing.redis.model;

import com.moilioncircle.redis.replicator.rdb.datatype.TTLValue;
import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * In-memory representation of a Redis hash: field names mapped to {@link RedisEntry}
 * wrappers that may carry per-field TTL.
 * <p>
 * Field keys are ordered with {@link RedisDataComparator}. Entries can be populated from
 * RDB replication events via {@link #putMapEntry(RedisDataSchema, Map.Entry)} and
 * {@link #putTtlMapEntry}.
 */
@JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ELEMENT, entriesWrapped = true)
public class RedisHash extends TreeMap<Object, RedisEntry> implements RedisValue {

    /** Creates an empty hash with {@link RedisDataComparator}-ordered keys. */
    public RedisHash() {
        super(new RedisDataComparator());
    }

    /**
     * Decodes and inserts a hash field from an RDB {@code HSET}-style entry.
     *
     * @param schema codecs for hash key and value
     * @param entry  raw field key and value bytes
     */
    public void putMapEntry(RedisDataSchema schema, Map.Entry<byte[], byte[]> entry) {
        Object key = schema.getHashKeyCodec().deserialize(entry.getKey());
        Object value = schema.getHashValueCodec().deserialize(entry.getValue());
        this.put(key, new RedisEntry(value));
    }

    /**
     * Inserts a decoded field without TTL metadata.
     *
     * @param key   field name
     * @param value field value
     */
    public void putMapEntry(Object key, Object value) {
        this.put(key, new RedisEntry(value));
    }

    /**
     * Decodes and inserts a hash field with per-field expiration from RDB replication.
     *
     * @param store  source of the current time for TTL calculation
     * @param schema codecs for hash key and value
     * @param entry  raw field key and TTL-wrapped value bytes
     */
    public void putTtlMapEntry(RedisInMemoryStore store, RedisDataSchema schema, Map.Entry<byte[], TTLValue> entry) {
        Object key = schema.getHashKeyCodec().deserialize(entry.getKey());
        Object value = schema.getHashValueCodec().deserialize(entry.getValue().getValue());
        RedisEntry e = new RedisEntry(value);
        e.setExpireAt(store.getNow(), entry.getValue().getExpires());
        this.put(key, e);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<Map.Entry<Object, RedisEntry>> nestedEntries() {
        return entrySet().stream();
    }
}
