package io.github.dimkich.integration.testing.redis.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;
import lombok.Getter;

public class AbstractEventMixIn {
    @JsonIgnore
    public Object context;
    @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
    public Object value;
    @JsonIgnore
    public long checksum;
    @JsonIgnore
    @Getter(onMethod_ = @JsonIgnore)
    public Long expiredMs;
    @JsonIgnore
    @Getter(onMethod_ = @JsonIgnore)
    public Integer expiredSeconds;
}
