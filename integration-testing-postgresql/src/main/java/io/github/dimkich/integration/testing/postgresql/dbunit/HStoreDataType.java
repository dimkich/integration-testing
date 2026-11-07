package io.github.dimkich.integration.testing.postgresql.dbunit;

import java.sql.Types;

/**
 * DBUnit data type of the PostgreSQL {@code hstore} type: the fixture value is a textual hstore
 * literal (e.g. <code>"k1"=>"v1", "k2"=>"v2"</code>) and is bound as a typed
 * {@link org.postgresql.util.PGobject}, so PostgreSQL parses the literal itself. Values may
 * therefore contain any character, including commas, colons and quotes.
 */
public class HStoreDataType extends PgObjectDataType {
    private static final String HSTORE = "hstore";

    public HStoreDataType() {
        super(HSTORE, Types.OTHER);
    }
}
