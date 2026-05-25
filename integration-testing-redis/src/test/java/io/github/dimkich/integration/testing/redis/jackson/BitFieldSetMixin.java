package io.github.dimkich.integration.testing.redis.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.redis.connection.BitFieldSubCommands;

@SuppressWarnings("unused")
public abstract class BitFieldSetMixin {
    @JsonCreator
    public static BitFieldSubCommands.BitFieldSet create(
            @JsonProperty("type") BitFieldSubCommands.BitFieldType type,
            @JsonProperty("offset") BitFieldSubCommands.Offset offset,
            @JsonProperty("value") long value) {
        return null;
    }

    @JsonIgnore
    abstract String getCommand();
}
