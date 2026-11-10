package io.github.dimkich.integration.testing.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.storage.mapping.MapContainer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@IntegrationTesting
@SpringBootTest(classes = ObjectsDifferenceTest.Config.class,
        properties = {"integration.testing.storage.storages.entries.default-key-type=OBJECT",
                "integration.testing.storage.storages.entries.default-change-type=true"})
public class ObjectsDifferenceTest {
    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("storage/objects-difference.xml");
    }

    @Configuration
    static class Config {
        @Bean
        TestSetupModule module() {
            return new TestSetupModule().addSubTypes(ScalarValue.class, StructuredValue.class,
                    StructuredValueWithoutVisitor.class, Item.class);
        }

        @Bean
        ObjectsDifferenceTestFacade objectsDifferenceTestFacade(ObjectsDifference objectsDifference) {
            return new ObjectsDifferenceTestFacade(objectsDifference);
        }
    }

    @RequiredArgsConstructor
    @SuppressWarnings("unused")
    public static class ObjectsDifferenceTestFacade {
        private final ObjectsDifference difference;

        public Object diffValue(Object left, Object right) {
            Object result = difference.getDifference("test",
                    Collections.singletonMap("value", left), Collections.singletonMap("value", right));
            if (result instanceof MapContainer<?, ?> container) {
                return container.getMap().get("value");
            }
            return null;
        }

        public Object diffMap(Object left, Object right) {
            return difference.getDifference("test", left, right);
        }

        public Object diffEntries(Object left, Object right) {
            return difference.getDifference("entries", left, right);
        }
    }

    @Data
    @JsonSerialize(using = ToStringSerializer.class)
    public static class ScalarValue {
        private String value;

        public ScalarValue() {
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public ScalarValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonSerialize(using = StructuredValueSerializer.class)
    public static class StructuredValue {
        private String payload;
    }

    public static class StructuredValueSerializer extends StdSerializer<StructuredValue> {
        public StructuredValueSerializer() {
            super(StructuredValue.class);
        }

        @Override
        public void serialize(StructuredValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("payload", value.getPayload());
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(StructuredValue value, JsonGenerator gen, SerializerProvider provider,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_OBJECT));
            gen.writeStringField("payload", value.getPayload());
            typeSer.writeTypeSuffix(gen, typeId);
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType type) throws JsonMappingException {
            visitor.expectObjectFormat(type);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonSerialize(using = StructuredValueWithoutVisitorSerializer.class)
    public static class StructuredValueWithoutVisitor {
        private String payload;
    }

    public static class StructuredValueWithoutVisitorSerializer extends StdSerializer<StructuredValueWithoutVisitor> {
        public StructuredValueWithoutVisitorSerializer() {
            super(StructuredValueWithoutVisitor.class);
        }

        @Override
        public void serialize(StructuredValueWithoutVisitor value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            gen.writeStartObject();
            gen.writeStringField("payload", value.getPayload());
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(StructuredValueWithoutVisitor value, JsonGenerator gen, SerializerProvider provider,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_OBJECT));
            gen.writeStringField("payload", value.getPayload());
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String name;
        private int count;
    }
}
