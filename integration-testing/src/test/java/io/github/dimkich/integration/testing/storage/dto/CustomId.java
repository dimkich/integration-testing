package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value with a custom equality predicate: two identifiers are equal when their values are equal,
 * whatever the tag.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomId {
    private String value;
    private String tag;
}
