package io.github.dimkich.integration.testing.redis.message;

import io.github.dimkich.integration.testing.message.ExceptionDto;
import io.github.dimkich.integration.testing.message.TestMessages;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaMetadata;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.serde.RedisDataCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * Captures Redis Pub/Sub pushes observed on the replication stream and puts them into the
 * {@link TestMessages} for assertions.
 * <p>
 * Invoked from the {@code PUBLISH}/{@code SPUBLISH} replication handlers for every connection.
 * The barrier channel is filtered out by {@link io.github.dimkich.integration.testing.redis.replication.RedisSyncBarrier}.
 * The channel name is decoded with the connection's key codec (channels are addressed like keys).
 * The channel is then matched against the regular key schemas of the connection
 * ({@link RedisDataSchemaRegistry}); channels marked with {@code ignore: true} are skipped.
 * The capture stores deserialization failures as an {@link ExceptionDto} with the raw bytes
 * preserved, like poison records in the Kafka module.
 *
 * @see io.github.dimkich.integration.testing.redis.replication.handler.stream.PublishCommandHandler
 */
@Slf4j
@RequiredArgsConstructor
public class RedisPushCapture {
    private final RedisDataSchemaRegistry schemaRegistry;
    private final TestMessages testMessages;

    /**
     * Deserializes a published payload and registers the push for assertions.
     *
     * @param store       in-memory store of the connection the push was observed on
     * @param channelRaw  raw channel bytes
     * @param payload     raw payload bytes
     */
    public void capture(RedisInMemoryStore store, byte[] channelRaw, byte[] payload) {
        if (store.getBarrier().isSyncChannel(channelRaw)) {
            return;
        }

        String connectionName = store.getName();
        String channel = schemaRegistry.findKeyCodec(connectionName).deserialize(channelRaw).toString();
        RedisDataSchemaMetadata metadata = schemaRegistry.findSchema(connectionName, channel);
        if (metadata.isIgnore()) {
            return;
        }

        RedisPush push = new RedisPush();
        push.setConnection(connectionName);
        push.setChannel(channel);
        push.setPayload(ByteBuffer.wrap(payload));
        try {
            RedisDataCodec valueCodec = metadata.getSchema().getValueCodec();
            push.setValue(valueCodec.deserialize(payload));
        } catch (Exception e) {
            log.warn("Failed to deserialize push message on channel [{}] for connection [{}]",
                    channel, connectionName, e);
            push.setValue(payload);
            push.setException(new ExceptionDto(e));
        }

        testMessages.putMessage(push, true);
        metadata.getFieldExclusion().process(push.getValue());
    }
}
