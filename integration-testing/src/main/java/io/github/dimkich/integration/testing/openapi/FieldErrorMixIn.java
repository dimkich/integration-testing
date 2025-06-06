package io.github.dimkich.integration.testing.openapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldErrorMixIn {
    @JsonCreator
    public FieldErrorMixIn(@JsonProperty("objectName") String objectName, @JsonProperty("field") String field,
                           @JsonProperty("rejectedValue") Object rejectedValue, @JsonProperty("bindingFailure") boolean bindingFailure,
                           @JsonProperty("codes") String[] codes, @JsonProperty("arguments") Object[] arguments,
                           @JsonProperty("defaultMessage") String defaultMessage) {
    }
}
