package io.github.dimkich.integration.testing.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.format.xml.attributes.BeanAsAttributes;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a message with headers and payload for integration testing.
 * <p>
 * This generic class is used to represent messages in integration tests, supporting both
 * inbound (received) and outbound (sent) messages. It provides structured access to message
 * metadata through headers and the actual message content through the payload.
 * <p>
 * The class is designed for serialization/deserialization from XML and JSON formats using
 * Jackson annotations. Headers are serialized as XML attributes when using {@link BeanAsAttributes},
 * while the payload is unwrapped in JSON representation.
 * <p>
 * Message headers can include standard properties such as topic, source, and key, as well as
 * custom headers.
 *
 * @author dimkich
 */
@Data
@NoArgsConstructor
@JsonRootName(value = "message")
public abstract class AbstractMessage {
    /**
     * The name of the Kafka connection (cluster) this message targets or originated from.
     * <p>
     * Used by multi-cluster senders to route messages to the correct broker.
     * Excluded from JSON/XML serialization as it is internal framework metadata.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String connection;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ExceptionDto exception;
}
