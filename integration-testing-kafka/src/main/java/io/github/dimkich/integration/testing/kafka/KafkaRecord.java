package io.github.dimkich.integration.testing.kafka;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.message.AbstractMessage;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link AbstractMessage} representation of a Kafka record, used both for inbound
 * messages captured by the sniffer and for outbound messages sent by the test.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class KafkaRecord extends AbstractMessage {
    @JacksonXmlProperty(isAttribute = true)
    private String topic;

    private Object key;

    private Object value;

    /**
     * Message headers as a multi-value map. Repeated header tags (or a JSON
     * array) accumulate into a value list; a single value is a list of one.
     */
    @JsonSerialize(as = LinkedMultiValueMapStringObject.class)
    @JsonDeserialize(as = LinkedMultiValueMapStringObject.class)
    private MultiValueMap<String, Object> headers = new LinkedMultiValueMap<>();

    private Integer partition;

    private Long offset;

    private Long timestamp;

    /**
     * Identifies the record by its position on the broker, as observed by the sniffer.
     */
    @Override
    public Object identity() {
        return new KafkaRecordIdentity(topic, partition, offset);
    }
}
