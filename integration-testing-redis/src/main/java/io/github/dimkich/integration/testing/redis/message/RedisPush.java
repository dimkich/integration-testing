package io.github.dimkich.integration.testing.redis.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.message.AbstractMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.nio.ByteBuffer;

/**
 * {@link AbstractMessage} representation of a Redis Pub/Sub push, used both for messages
 * captured by replication and for inbound messages sent by the test.
 * <p>
 * The channel is serialized as an XML attribute; the payload is serialized as the message
 * value. Raw payload bytes observed on the wire are kept separately in {@link #payload} to
 * build {@link RedisPushIdentity}, which lets {@link io.github.dimkich.integration.testing.message.TestMessages}
 * filter out messages produced by the test itself.
 *
 * @see RedisPushSender
 * @see RedisPushCapture
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RedisPush extends AbstractMessage {
    @JacksonXmlProperty(isAttribute = true)
    private String channel;

    private Object value;

    /**
     * Raw payload bytes as observed on the wire, used to identify the push on the transport.
     * Never serialized: equals, hashCode and toString are based on the payload content instead.
     */
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ByteBuffer payload;

    @Override
    public Object identity() {
        return new RedisPushIdentity(getConnection(), channel, payload);
    }
}
