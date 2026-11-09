package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Custom scalar serialized by {@link com.fasterxml.jackson.databind.ser.std.ToStringSerializer},
 * i.e. not by a {@code StdScalarSerializer} and not by a bean serializer.
 */
@Getter
@EqualsAndHashCode
public class ToStringScalar {
    private final String text;

    @JsonCreator
    public ToStringScalar(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
