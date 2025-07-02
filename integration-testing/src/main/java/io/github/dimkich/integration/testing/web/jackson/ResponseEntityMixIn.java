package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.MultiValueMap;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"statusCode", "statusCodeValue", "headers", "body"})
public class ResponseEntityMixIn {
    @JsonCreator
    public ResponseEntityMixIn(
            @JsonProperty("body") Object body,
            @JsonDeserialize(as = LinkedMultiValueMapStringString.class) @JsonProperty("headers") MultiValueMap<String, String> headers,
            @JsonProperty("statusCode") HttpStatusCode status) {
    }
}
