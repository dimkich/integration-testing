package io.github.dimkich.integration.testing.redis.jackson;

import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;

public class StreamMixIn {
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ELEMENT)
    public Object entries;
}
