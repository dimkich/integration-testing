package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped.UnwrappedTypeProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unwrapped polymorphic value whose type attribute is renamed on the declaring class.
 */
@Data
@NoArgsConstructor
@UnwrappedTypeProperty("kind")
public class UnwrappedTypeClass {
    @JsonUnwrapped
    private Object value;

    public static UnwrappedTypeClass of(Object value) {
        UnwrappedTypeClass instance = new UnwrappedTypeClass();
        instance.setValue(value);
        return instance;
    }
}
