package io.github.dimkich.integration.testing.xml.config.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.SneakyThrows;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@JsonSerialize(converter = SpringResourceMixIn.FromResource.class)
@JsonDeserialize(converter = SpringResourceMixIn.ToResource.class)
public class SpringResourceMixIn {
    public static class FromResource extends StdConverter<Resource, byte[]> {
        @Override
        @SneakyThrows
        public byte[] convert(Resource value) {
            return value.getContentAsByteArray();
        }
    }

    public static class ToResource extends StdConverter<byte[], Resource> {
        @Override
        public Resource convert(byte[] value) {
            return new ByteArrayResource(value);
        }
    }
}
