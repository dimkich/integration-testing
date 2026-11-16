package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base value: the presenter is registered for this type, so it applies to every subtype.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseValue {
    private String payload;
}
