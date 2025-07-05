package io.github.dimkich.integration.testing.postgresql.dbunit;

import lombok.extern.slf4j.Slf4j;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.TypeCastException;
import org.springframework.util.StringUtils;

import java.sql.*;

@Slf4j
public class ArrayDataType extends AbstractDataType {
    public ArrayDataType() {
        this("array", Types.ARRAY);
    }

    public ArrayDataType(String name, int sqlType) {
        super(name, sqlType, Array.class, false);
    }

    public Object typeCast(Object value) throws TypeCastException {
        if (value == null || value == ITable.NO_VALUE) {
            return null;
        }

        if (value instanceof String) {
            return new String[]{(String) value};
        }
        if (value instanceof String[]) {
            return value;
        }

        if (value instanceof Date || value instanceof Time || value instanceof Timestamp) {
            return new String[]{value.toString()};
        }

        if (value instanceof Boolean) {
            return new String[]{value.toString()};
        }

        if (value instanceof Number) {
            try {
                return new String[]{value.toString()};
            } catch (NumberFormatException e) {
                throw new TypeCastException(value, this, e);
            }
        }

        if (value instanceof Array) {
            try {
                Array a = (Array) value;
                return a.getArray();
            } catch (Exception e) {
                throw new TypeCastException(value, this, e);
            }
        }

        if (value instanceof Blob) {
            try {
                Blob blob = (Blob) value;
                byte[] blobValue = blob.getBytes(1, (int) blob.length());
                return typeCast(blobValue);
            } catch (SQLException e) {
                throw new TypeCastException(value, this, e);
            }
        }

        if (value instanceof Clob) {
            try {
                Clob clobValue = (Clob) value;
                int length = (int) clobValue.length();
                if (length > 0) {
                    return clobValue.getSubString(1, length);
                }
                return "";
            } catch (SQLException e) {
                throw new TypeCastException(value, this, e);
            }
        }

        log.warn("Unknown/unsupported object type '{}' - will invoke toString() as last fallback which " +
                "might produce undesired results", value.getClass().getName());
        return value.toString();
    }

    public Object getSqlValue(int column, ResultSet resultSet) throws SQLException {
        String value = resultSet.getString(column);
        if (value == null || resultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public void setSqlValue(Object value, int column, PreparedStatement statement) throws SQLException {
        Array array = statement.getConnection().createArrayOf("varchar", toArray(value));
        statement.setArray(column, array);
    }

    private Object[] toArray(Object value) {
        if (value instanceof String valueStr && StringUtils.hasText(valueStr)) {
            valueStr = valueStr.replaceAll("[{}]", "");
            valueStr = valueStr.replaceAll(", ", ",");
            return valueStr.split(",");
        }
        return new Object[]{};
    }
}