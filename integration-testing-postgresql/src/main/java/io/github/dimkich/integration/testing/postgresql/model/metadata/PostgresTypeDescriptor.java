package io.github.dimkich.integration.testing.postgresql.model.metadata;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import lombok.Value;

import java.util.List;
import java.util.Locale;

/**
 * PostgreSQL type information required by the audit record parser. Domains are resolved to their
 * base type while loading the metadata.
 *
 * <p>It also acts as the serde role carrying the descriptor through {@code SerdeManager}.
 */
@Value
public class PostgresTypeDescriptor implements ComponentRole {
    String name;
    char kind;
    PostgresTypeDescriptor element;
    List<PostgresCompositeField> fields;
    char delimiter;

    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the normalized key used by the PostgreSQL serde type predicates: a structural marker
     * for arrays and composite types, or the normalized PostgreSQL type name for scalar types.
     */
    public String getSerdeKey() {
        return switch (kind) {
            case 'a' -> "@array";
            case 'c' -> "@composite";
            default -> name.toLowerCase(Locale.ROOT);
        };
    }

    /**
     * Whether this descriptor represents the PostgreSQL {@code bytea} type.
     */
    public boolean isBytea() {
        return "bytea".equalsIgnoreCase(name);
    }

    /**
     * Whether this descriptor represents an array type.
     */
    public boolean isArray() {
        return kind == 'a';
    }
}
