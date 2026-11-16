package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

/**
 * Object with a map keyed by {@link BigDecimal}: the XML format writes the key as an attribute,
 * so the key type survives the round trip.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapHolder {
    @JsonProperty("entry")
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
    private LinkedHashMap<BigDecimal, String> entries;
}
