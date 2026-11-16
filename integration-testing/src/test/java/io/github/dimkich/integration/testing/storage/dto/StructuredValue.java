package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.dimkich.integration.testing.storage.jackson.StructuredValueSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value with its own structured serialization: the difference is built from its properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = StructuredValueSerializer.class)
public class StructuredValue {
    private String payload;
}
