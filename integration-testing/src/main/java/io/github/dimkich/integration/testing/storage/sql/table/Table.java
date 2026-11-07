package io.github.dimkich.integration.testing.storage.sql.table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Canonical identity of a table inside a {@link TableRegistry}. Besides the stable dense
 * {@link #getId()} and the external {@link TableName}, the instance owns the metadata of its storage
 * implementation. Instances are created only by the registry and compared by identity, so they can
 * be used directly in identity maps and indexed arrays.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class Table implements Comparable<Table> {
    private final int id;
    @NonNull
    private final TableName name;
    /**
     * Name used in human-readable diff keys. The default schema is omitted; other schemas are kept
     * so two tables with the same unqualified name do not collide.
     */
    private final String displayName;
    @Getter(AccessLevel.NONE)
    private String qualified;
    @Getter(AccessLevel.NONE)
    private String canonicalName;
    @Getter(AccessLevel.NONE)
    @Setter
    private TableMetadata metadata;

    public String getSchema() {
        return name.getSchema();
    }

    public String getTable() {
        return name.getTable();
    }

    public String getUnqualified() {
        return name.getUnqualified();
    }

    public String getQualified() {
        if (qualified == null) {
            qualified = name.getQualified();
        }
        return qualified;
    }

    public String getCanonicalName() {
        if (canonicalName == null) {
            canonicalName = name.getCanonicalName();
        }
        return canonicalName;
    }

    /**
     * Returns the attached metadata of the requested type.
     *
     * @throws IllegalStateException if no metadata has been prepared for the table
     */
    public <T extends TableMetadata> T getMetadata(Class<T> type) {
        if (metadata == null) {
            throw new IllegalStateException("Metadata " + type.getSimpleName()
                    + " is not prepared for table '" + this + "'");
        }
        return type.cast(metadata);
    }

    /**
     * Whether metadata has been attached to this table.
     */
    public boolean hasMetadata() {
        return metadata != null;
    }

    public void clearMetadata() {
        metadata = null;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(Table other) {
        return Integer.compare(id, other.id);
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
