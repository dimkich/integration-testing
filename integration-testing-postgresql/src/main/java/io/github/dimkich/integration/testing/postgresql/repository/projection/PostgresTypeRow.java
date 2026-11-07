package io.github.dimkich.integration.testing.postgresql.repository.projection;

import lombok.Value;

/**
 * Raw {@code pg_type} row used to build a {@code PostgresTypeDescriptor}.
 */
@Value
public class PostgresTypeRow {
    int oid;
    String name;
    char kind;
    int elementOid;
    int baseOid;
    int compositeOid;
    char delimiter;

    public boolean isDomain() {
        return kind == 'd' && baseOid != 0;
    }

    public boolean isArray() {
        return elementOid != 0;
    }

    public boolean isComposite() {
        return compositeOid != 0;
    }
}
