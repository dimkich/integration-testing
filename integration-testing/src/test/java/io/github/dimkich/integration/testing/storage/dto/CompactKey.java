package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Key with a custom object-key presenter: only the identifier is written, the second field is
 * intentionally hidden.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompactKey {
    private String id;
    private String ignored;
}
