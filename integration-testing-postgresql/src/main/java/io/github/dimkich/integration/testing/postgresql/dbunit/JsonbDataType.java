package io.github.dimkich.integration.testing.postgresql.dbunit;

import org.dbunit.dataset.datatype.AbstractDataType;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class JsonbDataType extends AbstractDataType {

    public JsonbDataType() {
        super("jsonb", Types.OTHER, String.class, false);
    }

    @Override
    public Object typeCast(Object obj) {
        return obj.toString();
    }

    @Override
    public Object getSqlValue(int column, ResultSet resultSet) throws SQLException {
        return resultSet.getString(column);
    }

    @Override
    public void setSqlValue(Object value,
                            int column,
                            PreparedStatement statement) throws SQLException {
        PGobject jsonObj = new PGobject();
        jsonObj.setType("json");
        jsonObj.setValue(value == null ? null : value.toString());

        statement.setObject(column, jsonObj);
    }
}
