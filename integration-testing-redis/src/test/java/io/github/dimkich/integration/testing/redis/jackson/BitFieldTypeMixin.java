package io.github.dimkich.integration.testing.redis.jackson;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.data.redis.connection.BitFieldSubCommands;

import java.io.IOException;

@JsonDeserialize(using = BitFieldTypeMixin.BitFieldTypeDeserializer.class)
public abstract class BitFieldTypeMixin {
    @JsonValue
    abstract String asString();

    static class BitFieldTypeDeserializer extends JsonDeserializer<BitFieldSubCommands.BitFieldType> {
        @Override
        public BitFieldSubCommands.BitFieldType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText();
            boolean signed = value.startsWith("i");
            int bits = Integer.parseInt(value.substring(1));
            return signed ? BitFieldSubCommands.BitFieldType.signed(bits) : BitFieldSubCommands.BitFieldType.unsigned(bits);
        }
    }
}
