package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTypeDescriptor;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeConverterFactory;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import io.github.dimkich.integration.testing.serde.impl.JacksonDeserializer;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Serde factory for PostgreSQL fields resolved without explicit configuration. The field type
 * descriptor is passed as the component role and the factory accepts any role of that class;
 * column overrides use {@code StandardSerdeProperties} and are handled only by the core factories.
 */
@Getter
public class PostgresFieldSerdeFactory
        implements TestSerdeConverterFactory<ByteBuffer, Object, PostgresColumnSerdeContext,
        PostgresTypeDescriptor, TestSerdeProperties.Default> {

    private static final PostgresHstoreConverter HSTORE = new PostgresHstoreConverter();
    private static final PostgresByteaConverter BYTEA = new PostgresByteaConverter();
    private static final PostgresBooleanConverter BOOLEAN = new PostgresBooleanConverter();
    private static final PostgresTemporalConverter TEMPORAL = new PostgresTemporalConverter();
    private static final PostgresShortConverter SHORT = new PostgresShortConverter();
    private static final PostgresIntConverter INT = new PostgresIntConverter();
    private static final PostgresLongConverter LONG = new PostgresLongConverter();
    private static final PostgresFloatConverter FLOAT = new PostgresFloatConverter();
    private static final PostgresDoubleConverter DOUBLE = new PostgresDoubleConverter();
    private static final PostgresBigDecimalConverter BIG_DECIMAL = new PostgresBigDecimalConverter();
    private static final PostgresStringConverter STRING = new PostgresStringConverter();

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<PostgresColumnSerdeContext> contextClass = PostgresColumnSerdeContext.class;
    private final Class<TestSerdeProperties.Default> propertiesClass = TestSerdeProperties.Default.class;
    private final Class<? extends PostgresTypeDescriptor> roleClass = PostgresTypeDescriptor.class;

    @Getter(AccessLevel.NONE)
    private final ObjectProvider<SerdeManager> serdeManager;
    @Getter(AccessLevel.NONE)
    private final PostgresJsonConverter jsonConverter;

    public PostgresFieldSerdeFactory(ObjectProvider<SerdeManager> serdeManager,
                                     JacksonDeserializer jsonDeserializer) {
        this.serdeManager = serdeManager;
        this.jsonConverter = new PostgresJsonConverter(jsonDeserializer);
    }

    @Override
    @Nullable
    public TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> create(
            TestSerdeProperties.Default config, Class<ByteBuffer> inputClass, Class<Object> outputClass,
            Class<PostgresColumnSerdeContext> contextClass, @Nullable PostgresTypeDescriptor type) {
        String key = type == null ? "@none" : type.getSerdeKey();
        return switch (key) {
            case "@none" -> null;
            case "@array" -> new PostgresArrayConverter(serdeManager, type);
            case "@composite" -> new PostgresCompositeConverter(serdeManager, type);
            case "json", "jsonb" -> jsonConverter;
            case "hstore" -> HSTORE;
            case "bytea" -> BYTEA;
            case "bool", "boolean" -> BOOLEAN;
            case "timestamp", "timestamptz", "date", "time", "timetz" -> TEMPORAL;
            case "int2", "smallint", "smallserial" -> SHORT;
            case "int4", "int", "integer", "serial" -> INT;
            case "int8", "bigint", "bigserial" -> LONG;
            case "float4", "real" -> FLOAT;
            case "float8", "double precision" -> DOUBLE;
            case "numeric", "decimal" -> BIG_DECIMAL;
            default -> STRING;
        };
    }
}
