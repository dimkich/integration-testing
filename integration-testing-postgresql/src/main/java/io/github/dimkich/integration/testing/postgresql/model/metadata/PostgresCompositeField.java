package io.github.dimkich.integration.testing.postgresql.model.metadata;

import lombok.Value;

/**
 * A field of a PostgreSQL composite type.
 */
@Value
public class PostgresCompositeField {
    String name;
    PostgresTypeDescriptor type;
}
