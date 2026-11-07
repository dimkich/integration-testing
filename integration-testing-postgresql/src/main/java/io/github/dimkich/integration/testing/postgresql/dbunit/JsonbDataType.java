package io.github.dimkich.integration.testing.postgresql.dbunit;

import java.sql.Types;

/**
 * DBUnit data type of the PostgreSQL {@code json}/{@code jsonb} types: the fixture value is a JSON
 * literal and is bound as a typed {@link org.postgresql.util.PGobject}, so PostgreSQL parses and
 * validates it.
 */
public class JsonbDataType extends PgObjectDataType {

    public JsonbDataType(String pgType) {
        super(pgType, Types.OTHER);
    }
}
