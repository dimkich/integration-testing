package io.github.dimkich.integration.testing.postgresql.dbunit;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.postgresql.GenericEnumType;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;

import java.sql.Types;

public class CustomPostgresqlDataTypeFactory extends PostgresqlDataTypeFactory {
    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        return switch (sqlType) {
            case Types.ARRAY -> new ArrayDataType();
            case Types.VARCHAR -> {
                if (sqlTypeName.equals("text") || sqlTypeName.equals("varchar")) {
                    yield super.createDataType(sqlType, sqlTypeName);
                }
                yield new GenericEnumType(sqlTypeName);
            }
            case Types.OTHER -> switch (sqlTypeName) {
                case "jsonb" -> new JsonbDataType();
                case "hstore" -> new HStoreDataType();
                default -> super.createDataType(sqlType, sqlTypeName);
            };
            default -> super.createDataType(sqlType, sqlTypeName);
        };
    }
}
