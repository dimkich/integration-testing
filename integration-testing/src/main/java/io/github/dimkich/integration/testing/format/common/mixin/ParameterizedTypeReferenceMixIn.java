package io.github.dimkich.integration.testing.format.common.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.github.dimkich.integration.testing.format.common.type.TypeGenerator;
import io.github.dimkich.integration.testing.format.common.type.TypeParser;
import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@JsonSerialize(using = ParameterizedTypeReferenceMixIn.Serializer.class)
@JsonDeserialize(using = ParameterizedTypeReferenceMixIn.Deserializer.class)
public class ParameterizedTypeReferenceMixIn {
    static class Serializer extends StdScalarSerializer<ParameterizedTypeReference> {
        private final TypeGenerator typeGenerator;

        public Serializer(TypeGenerator typeGenerator) {
            super(ParameterizedTypeReference.class);
            this.typeGenerator = typeGenerator;
        }

        @Override
        public void serialize(ParameterizedTypeReference value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(typeGenerator.generate(value.getType(), provider.getConfig()));
        }
    }

    static class Deserializer extends StdScalarDeserializer<ParameterizedTypeReference<?>> {
        private final TypeParser typeParser;
        private final Map<String, ParameterizedTypeReference<?>> typeCache = new ConcurrentHashMap<>();

        public Deserializer(TypeParser typeParser) {
            super(ParameterizedTypeReference.class);
            this.typeParser = typeParser;
        }

        @Override
        public ParameterizedTypeReference<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return typeCache.computeIfAbsent(p.getText(), t -> ParameterizedTypeReference.forType(typeParser.parse(t)));
        }
    }
}
