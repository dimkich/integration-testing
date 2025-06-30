package io.github.dimkich.integration.testing.web.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.nio.charset.Charset;

public class RestClientResponseExceptionMixin {
    @JsonCreator
    public RestClientResponseExceptionMixin(
            @JsonProperty(value = "message", required = true) String message,
            @JsonSerialize(contentAs = HttpStatus.class) @JsonProperty(value = "statusCode", required = true) HttpStatusCode statusCode,
            @JsonProperty(value = "statusText", required = true) String statusText,
            @JsonProperty("headers") HttpHeaders headers, @JsonProperty("responseBody") byte[] responseBody,
            @JsonProperty("responseCharset") Charset responseCharset) {
    }
}
