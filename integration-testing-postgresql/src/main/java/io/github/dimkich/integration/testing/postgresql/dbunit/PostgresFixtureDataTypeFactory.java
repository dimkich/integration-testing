package io.github.dimkich.integration.testing.postgresql.dbunit;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;

import java.sql.Types;

/**
 * DBUnit data type factory of the trigger based PostgreSQL storage: extends
 * {@link CustomPostgresDataTypeFactory} with the types its parent does not recognize.
 * <p>
 * Without the mapping DbUnit silently drops such columns from the table metadata, so they could not
 * be loaded from a fixture. The timezone-aware types are parsed with their explicit offset, so
 * fixture values do not depend on the JVM default timezone.
 */
public class PostgresFixtureDataTypeFactory extends CustomPostgresDataTypeFactory {

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if ("timestamptz".equals(sqlTypeName)) {
            return new TimestamptzDataType();
        }
        if ("timetz".equals(sqlTypeName)) {
            return new TimetzDataType();
        }
        if ("bit".equals(sqlTypeName) || "varbit".equals(sqlTypeName)) {
            return new BitStringDataType(sqlTypeName);
        }
        if (sqlType == Types.SQLXML) {
            return new XmlDataType();
        }
        return super.createDataType(sqlType, sqlTypeName);
    }
}
