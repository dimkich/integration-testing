package io.github.dimkich.integration.testing.storage.diff.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import io.github.dimkich.integration.testing.format.common.type.TypeGenerator;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import io.github.dimkich.integration.testing.util.FastPath;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a value the way the current test format does: as bare text and as a semantic type
 * name.
 * <p>
 * Text is produced by the serializer of the current format, so a value keeps its format specific
 * representation. The type name comes from {@link TypeGenerator} and is written as the
 * {@code type} attribute of a difference element.
 */
@RequiredArgsConstructor
public final class ValueFormat implements TypeNames {
    private final TypeGenerator typeGenerator;
    private final CompositeTestMapper testMapper;

    /**
     * Type names by class and mapper configuration. The name is resolved once per format instead
     * of on every element. XML and JSON can run in the same context, so the configuration is part
     * of the key: the first format does not fix the name for the other one.
     */
    private final ClassValue<Map<SerializationConfig, String>> typeNames = new ClassValue<>() {
        @Override
        protected Map<SerializationConfig, String> computeValue(@NonNull Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    /**
     * Renders a scalar value as bare text of the current format.
     * <p>
     * The serializer writes into a {@link TokenBuffer}, so the text is read from ready tokens
     * without an intermediate JSON document: a value keeps its text without quotes and escaping,
     * a binary value keeps its Base64 form.
     *
     * @param value value to render
     * @return rendered text
     * @throws IllegalStateException when the value is serialized as a structure rather than as a
     *                               scalar
     */
    public String text(Object value) {
        if (value instanceof String string) {
            return string;
        }
        SerializerProvider provider = testMapper.getSerializerProvider();
        try {
            JsonSerializer<Object> serializer = provider.findValueSerializer(value.getClass());
            String standardText = FastPath.standardText(serializer, value, provider);
            if (standardText != null) {
                return standardText;
            }
            TokenBuffer buffer = new TokenBuffer(testMapper.unwrap(), false);
            serializer.serialize(value, buffer, provider);
            try (JsonParser parser = buffer.asParserOnFirstToken()) {
                JsonToken token = parser.currentToken();
                if (token == null) {
                    return "";
                }
                if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                    throw new IllegalStateException("Value of type " + value.getClass().getName()
                            + " is serialized as a structure and cannot be written as text");
                }
                if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
                    return FastPath.embeddedText(parser.getEmbeddedObject(), provider);
                }
                return parser.getText();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot render value of type "
                    + value.getClass().getName(), e);
        }
    }

    /**
     * @param value value to inspect, may be {@code null}
     * @return type name, or {@code null} when the value has no explicit type
     */
    @Override
    public String typeOf(Object value) {
        if (value == null || value instanceof String) {
            return null;
        }
        SerializationConfig config = testMapper.unwrap().getSerializationConfig();
        Map<SerializationConfig, String> byConfig = typeNames.get(value.getClass());
        String name = byConfig.get(config);
        if (name != null) {
            return name;
        }
        String computed = typeGenerator.generate(value.getClass(), config);
        String previous = byConfig.putIfAbsent(config, computed);
        return previous == null ? computed : previous;
    }
}
