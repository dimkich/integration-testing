package io.github.dimkich.integration.testing.format.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holder for the {@code UnwrappedTypeProperty} tests: unwrapped values are nested into a bean,
 * like they are in real test declarations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnwrappedTypeHolder {
    private UnwrappedTypeField field;
    private UnwrappedTypeClass typedClass;
    private UnwrappedTypePrecedence precedence;
}
