package io.github.dimkich.integration.testing.format.common.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter(onMethod_ = @JsonIgnore)
@Setter(onMethod_ = @JsonIgnore)
public class ThrowableMixIn {
    private Object stackTrace;
    private Object cause;
    private Object rootCause;
    private Object mostSpecificCause;
    private Object localizedMessage;
    private Object constraintViolations;
    private Object responseBodyAsString;
    private Object responseBodyAsByteArray;
    @Getter
    @Setter
    @JsonProperty("statusCodeValue")
    private Object rawStatusCode;
}
