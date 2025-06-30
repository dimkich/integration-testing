package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Type;
import java.net.URI;

@Getter(onMethod_ = @JsonIgnore)
@Setter(onMethod_ = @JsonIgnore)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"url", "method", "headers", "body"})
public class RequestEntityMixIn {
    private Type type;

    @JsonCreator
    public RequestEntityMixIn(
            @JsonProperty("body") Object body,
            @JsonDeserialize(using = MultiValueMapDeserializer.class) @JsonProperty("headers") MultiValueMap<String, String> headers,
            @JsonProperty("method") HttpMethod method,
            @JsonProperty("url") URI url) {
    }
}
