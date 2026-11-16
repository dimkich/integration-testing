package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.dimkich.integration.testing.storage.jackson.StructuredValueWithoutVisitorSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value whose serializer does not declare its format through a format visitor: the difference is
 * still built from its properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = StructuredValueWithoutVisitorSerializer.class)
public class StructuredValueWithoutVisitor {
    private String payload;
}
