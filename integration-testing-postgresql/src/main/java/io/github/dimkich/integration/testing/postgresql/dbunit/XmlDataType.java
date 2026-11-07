package io.github.dimkich.integration.testing.postgresql.dbunit;

import java.sql.Types;

/**
 * DBUnit data type of PostgreSQL {@code xml}: the fixture value is bound as a
 * {@link org.postgresql.util.PGobject} of type {@code xml}, because the driver does not accept a
 * plain string for xml columns.
 */
public class XmlDataType extends PgObjectDataType {

    public XmlDataType() {
        super("xml", Types.SQLXML);
    }
}
