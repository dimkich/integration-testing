package io.github.dimkich.integration.testing.format.dto;

import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.TreeMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookWrapped {
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE, entriesWrapped = true)
    private TreeMap<BigDecimal, BigDecimal> bids;
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE, entriesWrapped = true)
    private TreeMap<BigDecimal, BigDecimal> offers;
}
