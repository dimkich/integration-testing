package io.github.dimkich.integration.testing.postgresql.dbunit;

import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.AbstractDataType;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DBUnit data type that binds a textual PostgreSQL literal as a typed {@link PGobject}. The
 * concrete type supplies the PostgreSQL type name and the JDBC type used for null values.
 */
abstract class PgObjectDataType extends AbstractDataType {
    private final String pgType;
    private final int sqlType;

    protected PgObjectDataType(String pgType, int sqlType) {
        super(pgType, sqlType, String.class, false);
        this.pgType = pgType;
        this.sqlType = sqlType;
    }

    @Override
    public Object typeCast(Object value) {
        if (value == null || value == ITable.NO_VALUE) {
            return null;
        }
        return value.toString();
    }

    @Override
    public Object getSqlValue(int column, ResultSet resultSet) throws SQLException {
        String value = resultSet.getString(column);
        if (value == null || resultSet.wasNull()) {
            return null;
        }
        return value;
    }

    @Override
    public void setSqlValue(Object value, int column, PreparedStatement statement)
            throws SQLException {
        Object cast = typeCast(value);
        if (cast == null) {
            statement.setNull(column, sqlType);
            return;
        }
        PGobject object = new PGobject();
        object.setType(pgType);
        object.setValue(cast.toString());
        statement.setObject(column, object);
    }
}
