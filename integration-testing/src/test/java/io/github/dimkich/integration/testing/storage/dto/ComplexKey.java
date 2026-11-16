package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite key of a map: the key is written as a structure rather than as a line of text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplexKey {
    private String exchange;
    private String classCode;
    private String ticker;
}
