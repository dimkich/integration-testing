package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.model.RedisStreamEntry;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.ByteRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link RedisDataAccessor} that writes {@link RedisStream} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then appends each
 * {@link RedisStreamEntry} with {@code XADD}, using hash field and value codecs
 * from {@link RedisDataSchema}.
 */
public class StreamDataAccessor implements RedisDataAccessor<RedisStream> {

    /** {@inheritDoc} */
    @Override
    public Class<RedisStream> getSupportedClass() {
        return RedisStream.class;
    }

    /**
     * Writes a stream to Redis, replacing any existing value at {@code key}.
     * <p>
     * Empty streams only delete the key. Non-empty streams are stored entry by entry
     * with {@code XADD}; each entry keeps its model id and field map order is preserved
     * via a {@link LinkedHashMap} before serialization.
     *
     * @param key    serialized Redis key
     * @param data   stream entries to store
     * @param conn   connection used for commands
     * @param schema codecs for stream field names and values
     */
    @Override
    public void store(byte[] key, RedisStream data, RedisConnection conn, RedisDataSchema schema) {
        conn.keyCommands().del(key);
        for (RedisStreamEntry record : data) {
            Map<byte[], byte[]> fields = new LinkedHashMap<>();
            record.getFields().forEach((f, v) -> fields.put(
                    schema.getHashKeyCodec().serialize(f),
                    schema.getHashValueCodec().serialize(v)));

            ByteRecord byteRecord = StreamRecords.newRecord()
                    .in(key)
                    .withId(record.getId())
                    .ofBytes(fields);

            conn.streamCommands().xAdd(byteRecord);
        }
    }
}

