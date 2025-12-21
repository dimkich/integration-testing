package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.TreeMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBook {
    @JsonProperty("bid")
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
    private TreeMap<BigDecimal, BigDecimal> bids;
    @JsonProperty("offer")
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
    private TreeMap<BigDecimal, BigDecimal> offers;
}
