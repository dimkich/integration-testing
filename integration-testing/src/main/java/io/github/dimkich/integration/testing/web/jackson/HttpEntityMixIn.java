package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.util.MultiValueMap;

public class HttpEntityMixIn {
    @JsonCreator
    public HttpEntityMixIn(
            @JsonProperty("body") Object body,
            @JsonDeserialize(as = LinkedMultiValueMapStringString.class)
            @JsonProperty("headers") MultiValueMap<String, String> headers) {
    }
}
