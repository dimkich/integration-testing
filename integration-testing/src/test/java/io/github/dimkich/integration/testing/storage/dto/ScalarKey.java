package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Key type serialized as a scalar by {@code @JsonValue}: the text form comes from the format
 * serializer, not from {@code toString()}.
 */
@Data
@NoArgsConstructor
public class ScalarKey {
    @JsonValue
    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ScalarKey(String value) {
        this.value = value;
    }
}
