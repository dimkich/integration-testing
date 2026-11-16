package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.nio.charset.Charset;

/**
 * Jackson mix-in for {@link org.springframework.web.client.RestClientResponseException}.
 * <p>
 * A throwable is written with its message only (see
 * {@code ThrowableMixIn}), so the properties required by the creator have to be declared here
 * explicitly: without them the expected test data cannot be read back.
 */
@JsonPropertyOrder({"message", "statusCode", "statusText"})
public abstract class RestClientResponseExceptionMixin {
    @JsonCreator
    public RestClientResponseExceptionMixin(
            @JsonProperty(value = "message", required = true) String message,
            @JsonSerialize(contentAs = HttpStatus.class) @JsonProperty(value = "statusCode", required = true) HttpStatusCode statusCode,
            @JsonProperty(value = "statusText", required = true) String statusText,
            @JsonProperty("headers") HttpHeaders headers, @JsonProperty("responseBody") byte[] responseBody,
            @JsonProperty("responseCharset") Charset responseCharset) {
    }

    @JsonProperty("statusCode")
    public abstract HttpStatusCode getStatusCode();

    @JsonProperty("statusText")
    public abstract String getStatusText();
}
