package io.github.dimkich.integration.testing.redis.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.springframework.data.redis.connection.BitFieldSubCommands;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(converter = BitFieldSubCommandsMixin.BitFieldSubCommandsConverter.class)
public class BitFieldSubCommandsMixin {
    public static class BitFieldSubCommandsConverter extends StdConverter<BitFieldSubCommandsDTO, BitFieldSubCommands> {
        @Override
        public BitFieldSubCommands convert(BitFieldSubCommandsDTO dto) {
            return BitFieldSubCommands.create(dto.subCommands.toArray(new BitFieldSubCommands.BitFieldSubCommand[0]));
        }
    }

    public static class BitFieldSubCommandsDTO {
        public final List<BitFieldSubCommands.BitFieldSubCommand> subCommands = new ArrayList<>();
    }
}
