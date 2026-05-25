package io.github.dimkich.integration.testing.redis.model;

import com.moilioncircle.redis.replicator.rdb.datatype.Stream;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory representation of a Redis stream: an ordered list of {@link RedisStreamEntry}
 * records.
 * <p>
 * Entries appended from RDB replication have field names and values decoded with the
 * schema's {@link RedisDataSchema#getValueCodec() value codec}.
 */
public class RedisStream extends ArrayList<RedisStreamEntry> implements RedisValue {

    /**
     * Appends a decoded stream entry from an RDB replication event.
     *
     * @param schema codecs for stream field names and values
     * @param entry  raw stream entry from the replicator
     */
    public void addEntry(RedisDataSchema schema, Stream.Entry entry) {
        Map<Object, Object> decodedFields = new LinkedHashMap<>();
        entry.getFields().forEach((f, v) -> {
            Object field = schema.getValueCodec().deserialize(f);
            Object value = schema.getValueCodec().deserialize(v);
            decodedFields.put(field, value);
        });
        this.add(new RedisStreamEntry(entry.getId().toString(), decodedFields));
    }
}
