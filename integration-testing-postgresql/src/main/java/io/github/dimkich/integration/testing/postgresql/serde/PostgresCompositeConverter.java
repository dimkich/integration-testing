package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.postgresql.model.PostgresTupleLayout;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresCompositeField;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTypeDescriptor;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a PostgreSQL composite value into a field-name to value map. Field converters are
 * resolved lazily once per composite type, and the tuple layout preserves {@code NULL} fields.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class PostgresCompositeConverter extends PostgresConverter {

    private final ObjectProvider<SerdeManager> serdeManager;
    private final PostgresTypeDescriptor type;
    private List<TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext>> fieldConverters;

    @Override
    public Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        if (input == null) {
            return null;
        }
        ByteBuffer[] values = new PostgresTupleLayout(input, type.getFields().size(),
                context.getTableName() + "." + context.getColumnName()).getFields();
        List<PostgresCompositeField> fields = type.getFields();
        Map<String, Object> result = new LinkedHashMap<>(fields.size());
        for (int i = 0; i < fields.size(); i++) {
            PostgresCompositeField field = fields.get(i);
            ByteBuffer value = values[i];
            if (value == null) {
                result.put(field.getName(), null);
                continue;
            }
            PostgresColumnSerdeContext fieldContext = context.getFieldContext(i);
            result.put(field.getName(), fieldConverter(i).convert(value, fieldContext));
        }
        return result;
    }

    private TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> fieldConverter(int index) {
        if (fieldConverters == null) {
            SerdeManager manager = serdeManager.getObject();
            List<TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext>> converters =
                    new ArrayList<>(type.getFields().size());
            for (PostgresCompositeField field : type.getFields()) {
                converters.add(manager.resolve(TestSerdeProperties.Default.INSTANCE,
                        ByteBuffer.class, Object.class, PostgresColumnSerdeContext.class, field.getType()));
            }
            fieldConverters = converters;
        }
        return fieldConverters.get(index);
    }
}
