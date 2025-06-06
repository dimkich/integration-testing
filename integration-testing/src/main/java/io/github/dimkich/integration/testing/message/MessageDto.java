package io.github.dimkich.integration.testing.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dimkich.integration.testing.xml.attributes.BeanAsAttributes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @JsonCreator(mode = JsonCreator.Mode.DISABLED))
@JsonRootName(value = "message")
public class MessageDto<T> {
    public static final String TEST_INBOUND_MESSAGE = "TEST_INBOUND_MESSAGE";
    private static Set<String> kafkaIgnoredHeaders = Set.of("id", "timestamp", "kafka_consumer", "kafka_timestampType",
            "kafka_receivedPartitionId", "kafka_receivedTimestamp", "__TypeId__", "kafka_groupId", "kafka_offset",
            "kafka_acknowledgment");
    @BeanAsAttributes
    private MessageHeadersDto headers = new MessageHeadersDto();
    @JsonUnwrapped
    private T payload;
    @JsonIgnore
    private boolean testInboundMessage;

    public MessageDto(T payload) {
        this.payload = payload;
    }
}
