package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value with a composite key: the key presenter joins the name and the count. The note is not part
 * of the key, so two items with the same name and count render as the same key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    private String name;
    private int count;
    private String note;
}
