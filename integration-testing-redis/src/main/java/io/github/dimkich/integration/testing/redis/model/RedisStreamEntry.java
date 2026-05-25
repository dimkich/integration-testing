package io.github.dimkich.integration.testing.redis.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;
import lombok.Data;

import java.util.Map;

/**
 * A single record in a {@link RedisStream}, identified by its stream ID and field map.
 */
@Data
public class RedisStreamEntry {
    @JacksonXmlProperty(isAttribute = true)
    private final String id;
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ELEMENT)
    private final Map<Object, Object> fields;
}
