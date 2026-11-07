package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import org.springframework.lang.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test-only provider that replaces the former PostgreSQL point normalizer SPI.
 */
public final class PostgresPointProvider implements TestSerdeProviderFactory<ByteBuffer, Object,
        PostgresColumnSerdeContext, ComponentRole, StandardSerdeProperties> {

    @Override
    public String getName() {
        return "it-point";
    }

    @Override
    public Class<ByteBuffer> getInputClass() {
        return ByteBuffer.class;
    }

    @Override
    public Class<Object> getOutputClass() {
        return Object.class;
    }

    @Override
    public Class<PostgresColumnSerdeContext> getContextClass() {
        return PostgresColumnSerdeContext.class;
    }

    @Override
    public Class<StandardSerdeProperties> getPropertiesClass() {
        return StandardSerdeProperties.class;
    }

    @Override
    public TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> create(
            StandardSerdeProperties config, Class<ByteBuffer> inputClass, Class<Object> outputClass,
            Class<PostgresColumnSerdeContext> contextClass, @Nullable ComponentRole role) {
        return TestSerdeConverter.of(ByteBuffer.class, Object.class, PostgresColumnSerdeContext.class,
                (input, context) -> {
                    String value = StandardCharsets.UTF_8.decode(input.duplicate()).toString();
                    Map<String, Object> normalized = new LinkedHashMap<>();
                    if (value.startsWith("(") && value.endsWith(")")) {
                        String[] coordinates = value.substring(1, value.length() - 1).split(",");
                        normalized.put("x", Double.parseDouble(coordinates[0].trim()));
                        normalized.put("y", Double.parseDouble(coordinates[1].trim()));
                    } else {
                        normalized.put("value", value);
                    }
                    normalized.put("table", context.getTableName());
                    normalized.put("column", context.getColumnName());
                    return normalized;
                });
    }
}
