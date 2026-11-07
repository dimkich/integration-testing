package io.github.dimkich.integration.testing.postgresql.dbunit;

import io.github.dimkich.integration.testing.postgresql.PostgresTemporalText;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.TypeCastException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * DBUnit data type of PostgreSQL {@code timestamptz}: fixture values carry an explicit offset
 * (e.g. {@code 2026-01-01 10:15:30+03:00}) and are bound as {@link OffsetDateTime}, so the loaded
 * instant does not depend on the JVM default timezone. Values without an offset are interpreted in
 * the JVM default timezone, like the plain JDBC driver does.
 */
public class TimestamptzDataType extends AbstractDataType {

    public TimestamptzDataType() {
        super("timestamptz", Types.TIMESTAMP_WITH_TIMEZONE, OffsetDateTime.class, false);
    }

    @Override
    public Object typeCast(Object value) throws TypeCastException {
        if (value == null || value == ITable.NO_VALUE) {
            return null;
        }
        if (value instanceof OffsetDateTime) {
            return value;
        }
        String text = value.toString().trim().replace(' ', 'T');
        try {
            return OffsetDateTime.parse(PostgresTemporalText.normalizeOffset(text));
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toOffsetDateTime();
            } catch (DateTimeParseException ex) {
                throw new TypeCastException(value, this, ex);
            }
        }
    }

    @Override
    public void setSqlValue(Object value, int column, PreparedStatement statement)
            throws SQLException, TypeCastException {
        statement.setObject(column, typeCast(value));
    }
}
