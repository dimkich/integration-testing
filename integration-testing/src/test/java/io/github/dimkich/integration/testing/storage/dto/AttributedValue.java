package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value with a configurable metadata attribute: the tests use it to check attribute names that
 * the XML difference format reserves or cannot represent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributedValue {
    private String body;
    private String attributeName;
    private String attributeValue;
}
