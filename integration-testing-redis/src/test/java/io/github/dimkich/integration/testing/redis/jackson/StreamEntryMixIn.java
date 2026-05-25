package io.github.dimkich.integration.testing.redis.jackson;

import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;

public class StreamEntryMixIn {
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
    public Object fields;
}
