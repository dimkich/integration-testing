package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped.UnwrappedTypeProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The field-level {@link UnwrappedTypeProperty} must take precedence over the class-level one.
 */
@Data
@NoArgsConstructor
@UnwrappedTypeProperty("classKind")
public class UnwrappedTypePrecedence {
    @JsonUnwrapped
    @UnwrappedTypeProperty("fieldKind")
    private Object value;

    public static UnwrappedTypePrecedence of(Object value) {
        UnwrappedTypePrecedence instance = new UnwrappedTypePrecedence();
        instance.setValue(value);
        return instance;
    }
}
