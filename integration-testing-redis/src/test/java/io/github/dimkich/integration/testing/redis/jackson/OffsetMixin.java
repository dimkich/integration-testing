package io.github.dimkich.integration.testing.redis.jackson;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.data.redis.connection.BitFieldSubCommands;

import java.io.IOException;

@JsonDeserialize(using = OffsetMixin.OffsetDeserializer.class)
public abstract class OffsetMixin {
    @JsonValue
    abstract String asString();

    static class OffsetDeserializer extends JsonDeserializer<BitFieldSubCommands.Offset> {
        @Override
        public BitFieldSubCommands.Offset deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText();
            if (value == null || value.isEmpty()) {
                return null;
            }
            if (value.startsWith("#")) {
                long val = Long.parseLong(value.substring(1));
                return BitFieldSubCommands.Offset.offset(val).multipliedByTypeLength();
            } else {
                long val = Long.parseLong(value);
                return BitFieldSubCommands.Offset.offset(val);
            }
        }
    }
}
