package io.github.dimkich.integration.testing.postgresql.dbunit;

import io.github.dimkich.integration.testing.postgresql.PostgresTemporalText;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.TypeCastException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.DateTimeException;
import java.time.OffsetTime;

/**
 * DBUnit data type of PostgreSQL {@code timetz}: fixture values carry an explicit offset
 * (e.g. {@code 10:15:30+03:00}) and are bound as {@link OffsetTime}, so the loaded value does not
 * depend on the JVM default timezone.
 */
public class TimetzDataType extends AbstractDataType {

    public TimetzDataType() {
        super("timetz", Types.TIME_WITH_TIMEZONE, OffsetTime.class, false);
    }

    @Override
    public Object typeCast(Object value) throws TypeCastException {
        if (value == null || value == ITable.NO_VALUE) {
            return null;
        }
        if (value instanceof OffsetTime) {
            return value;
        }
        try {
            return OffsetTime.parse(PostgresTemporalText.normalizeOffset(value.toString()));
        } catch (DateTimeException e) {
            throw new TypeCastException(value, this, e);
        }
    }

    @Override
    public void setSqlValue(Object value, int column, PreparedStatement statement)
            throws SQLException, TypeCastException {
        statement.setObject(column, typeCast(value));
    }
}
