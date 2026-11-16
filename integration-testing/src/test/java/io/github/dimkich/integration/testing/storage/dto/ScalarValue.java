package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * Value with its own text representation: the format writes it as text, so a difference keeps the
 * value as a scalar.
 */
@Data
@JsonSerialize(using = ToStringSerializer.class)
public class ScalarValue {
    private String value;

    public ScalarValue() {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ScalarValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
