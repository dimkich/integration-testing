package io.github.dimkich.integration.testing.postgresql.dbunit;

import java.sql.Types;

/**
 * DBUnit data type of the PostgreSQL array types: the fixture value is a textual array literal
 * (e.g. <code>{tag_x,tag_y}</code>) and is bound as a typed {@link org.postgresql.util.PGobject},
 * so PostgreSQL parses the literal itself. Element values may therefore contain any character,
 * including commas, braces and quotes.
 */
public class ArrayDataType extends PgObjectDataType {

    public ArrayDataType(String pgType) {
        super(pgType, Types.ARRAY);
    }

    /**
     * Converts the type name reported by the driver to the form PostgreSQL accepts, e.g. the
     * internal {@code _text} to {@code text[]}.
     */
    public static String typeName(String sqlTypeName) {
        if (sqlTypeName == null || sqlTypeName.isBlank()) {
            return "text[]";
        }
        return sqlTypeName.charAt(0) == '_' ? sqlTypeName.substring(1) + "[]" : sqlTypeName;
    }
}
