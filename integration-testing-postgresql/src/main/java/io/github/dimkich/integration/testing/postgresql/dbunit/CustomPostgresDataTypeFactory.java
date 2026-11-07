package io.github.dimkich.integration.testing.postgresql.dbunit;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.postgresql.GenericEnumType;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;

import java.sql.Types;

/**
 * DBUnit data type factory for the PostgreSQL types that the standard factory does not map
 * correctly: arrays, enums, {@code json}/{@code jsonb}, and {@code hstore}.
 */
public class CustomPostgresDataTypeFactory extends PostgresqlDataTypeFactory {
    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        return switch (sqlType) {
            case Types.ARRAY -> new ArrayDataType(ArrayDataType.typeName(sqlTypeName));
            case Types.VARCHAR -> {
                if ("text".equals(sqlTypeName) || "varchar".equals(sqlTypeName)) {
                    yield super.createDataType(sqlType, sqlTypeName);
                }
                yield new GenericEnumType(sqlTypeName);
            }
            case Types.OTHER -> switch (sqlTypeName) {
                case "json" -> new JsonbDataType("json");
                case "jsonb" -> new JsonbDataType("jsonb");
                case "hstore" -> new HStoreDataType();
                default -> super.createDataType(sqlType, sqlTypeName);
            };
            default -> super.createDataType(sqlType, sqlTypeName);
        };
    }
}
