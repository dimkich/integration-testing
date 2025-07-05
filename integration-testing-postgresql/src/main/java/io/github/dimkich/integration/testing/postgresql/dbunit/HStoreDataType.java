package io.github.dimkich.integration.testing.postgresql.dbunit;

import org.dbunit.dataset.datatype.AbstractDataType;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;

public class HStoreDataType extends AbstractDataType {
    public HStoreDataType() {
        super("hstore", Types.OTHER, HStoreDataType.class, false);
    }

    public HStoreDataType(String name, int sqlType) {
        super(name, sqlType, HStoreDataType.class, false);
    }

    public Object typeCast(Object value) {
        return value;
    }

    public Object getSqlValue(int column, ResultSet resultSet) throws SQLException {
        String value = resultSet.getString(column);
        if (value == null || resultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public void setSqlValue(Object value, int column, PreparedStatement statement) throws SQLException {
        statement.setObject(column, toMap(value));
    }

    private Map<String, String> toMap(Object value) {
        Map<String, String> map = new LinkedHashMap<>();
        if (value instanceof String valueStr) {
            if (StringUtils.hasText(valueStr)) {
                valueStr = valueStr.replaceAll("=>", ":");
                valueStr = valueStr.replaceAll("->", ":");
                valueStr = valueStr.replaceAll("=", ":");
                valueStr = valueStr.replaceAll("[{}\"]", "");
                valueStr = valueStr.replaceAll(", ", ",");
                String[] split = valueStr.split(",");
                for (String str : split) {
                    String[] keyValue = str.split(":");
                    if (keyValue.length == 1) {
                        map.put(keyValue[0], null);
                    } else if (keyValue.length == 2) {
                        map.put(keyValue[0], keyValue[1]);
                    }
                }
            }
        }
        return map;
    }
}
