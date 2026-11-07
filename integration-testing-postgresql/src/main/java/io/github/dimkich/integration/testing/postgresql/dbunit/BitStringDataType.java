package io.github.dimkich.integration.testing.postgresql.dbunit;

import java.sql.Types;

/**
 * DBUnit data type of the PostgreSQL {@code bit}/{@code varbit} types: the fixture value is bound
 * as a typed {@link org.postgresql.util.PGobject}, so a bit string is not loaded as a boolean.
 */
public class BitStringDataType extends PgObjectDataType {

    public BitStringDataType(String pgType) {
        super(pgType, Types.OTHER);
    }
}
