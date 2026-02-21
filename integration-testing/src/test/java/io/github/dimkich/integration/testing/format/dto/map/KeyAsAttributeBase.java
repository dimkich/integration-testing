package io.github.dimkich.integration.testing.format.dto.map;

import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;
import lombok.Data;

@Data
public class KeyAsAttributeBase<T> {
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
    private T val;
}
