package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fields reordered by {@link JsonPropertyOrder}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"second", "first"})
public class AnnotatedOrder {
    private String first;
    private String second;
}
