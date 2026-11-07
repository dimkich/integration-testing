package io.github.dimkich.integration.testing.postgresql.repository.projection;

import lombok.Value;

/**
 * Raw attribute row of a PostgreSQL composite type.
 */
@Value
public class PostgresCompositeFieldRow {
    int compositeOid;
    String name;
    int typeOid;
}
