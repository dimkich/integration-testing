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

import java.io.IOException;
import java.lang.reflect.Type;

@JsonSerialize(using = TypeMixin.Serializer.class)
@JsonDeserialize(using = TypeMixin.Deserializer.class)
public class TypeMixin {
    static class Serializer extends StdScalarSerializer<Type> {
        private final TypeGenerator typeGenerator;

        public Serializer(TypeGenerator typeGenerator) {
            super(Type.class);
            this.typeGenerator = typeGenerator;
        }

        @Override
        public void serialize(Type value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(typeGenerator.generate(value, provider.getConfig()));
        }
    }

    static class Deserializer extends StdScalarDeserializer<Type> {
        private final TypeParser typeParser;

        public Deserializer(TypeParser typeParser) {
            super(Type.class);
            this.typeParser = typeParser;
        }

        @Override
        public Type deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return typeParser.parse(p.getText());
        }
    }
}
