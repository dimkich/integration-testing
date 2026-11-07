package io.github.dimkich.integration.testing.postgresql.model.change;

import io.github.dimkich.integration.testing.postgresql.model.PostgresTupleLayout;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * One raw PostgreSQL row version used by the Smart Revert. The original tuple buffer is kept for
 * exact SQL rendering, while fields are materialized from a lazy {@link PostgresTupleLayout}.
 * <p>
 * Equality is full row equality: table and raw tuple. The grouping identity used by the audit is
 * exposed separately by {@link #getRowKey()}; a table with a primary key groups by its primary-key
 * fields, while a table without one groups by the complete tuple.
 */
@EqualsAndHashCode(of = {"table", "data"})
@RequiredArgsConstructor
public final class PostgresRawRow {
    private static final ByteBuffer[] EMPTY_FIELDS = new ByteBuffer[0];

    @Getter
    @NonNull
    private final Table table;
    @NonNull
    private final ByteBuffer data;
    private PostgresTupleLayout layout;
    private ByteBuffer[] identityAlwaysFields;
    private PostgresRawKey rowKey;

    public String getTupleText() {
        if (data.hasArray()) {
            return new String(data.array(), data.arrayOffset() + data.position(), data.remaining(),
                    StandardCharsets.UTF_8);
        }
        return StandardCharsets.UTF_8.decode(data.duplicate()).toString();
    }

    /**
     * SQL tuple literal with a dollar-quote tag that does not occur in the tuple text, cast to the
     * qualified table type.
     */
    public String toSqlTuple() {
        String tuple = getTupleText();
        String tag = "$itrow$";
        int suffix = 0;
        while (tuple.contains(tag)) {
            tag = "$itrow" + (++suffix) + "$";
        }
        String qualified = table.getQualified();
        return tag + tuple + tag + "::" + qualified;
    }

    ByteBuffer getTupleData() {
        return data.asReadOnlyBuffer();
    }

    /**
     * Identity of this row inside its table state. The key is built lazily and reused by both the
     * endpoint-state grouping and the display key.
     */
    public PostgresRawKey getRowKey() {
        if (rowKey == null) {
            rowKey = createRowKey();
        }
        return rowKey;
    }

    private PostgresRawKey createRowKey() {
        PostgresTableMetadata metadata = table.getMetadata(PostgresTableMetadata.class);
        int[] indexes = metadata.getPrimaryKeyIndexes();
        if (indexes.length == 0) {
            return new PostgresRawKey(table, false, new ByteBuffer[]{getTupleData()});
        }
        ByteBuffer[] fields = new ByteBuffer[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            fields[i] = getField(indexes[i]);
        }
        return new PostgresRawKey(table, true, fields);
    }

    /**
     * Display key of this row in the difference engine and test XML.
     */
    public String getDisplayKey(Map<String, ?> values) {
        return getRowKey().getDisplayKey(values);
    }

    /**
     * Whether both rows belong to the same table and have the same identity-always values.
     */
    boolean hasSameIdentityAlways(PostgresRawRow other) {
        return table.equals(other.table)
                && Arrays.equals(getIdentityAlwaysFields(), other.getIdentityAlwaysFields());
    }

    public ByteBuffer[] getFields() {
        return getLayout().getFields();
    }

    ByteBuffer getField(int index) {
        return getLayout().getField(index);
    }

    private PostgresTupleLayout getLayout() {
        if (layout == null) {
            layout = new PostgresTupleLayout(data,
                    table.getMetadata(PostgresTableMetadata.class).getColumns().size(),
                    table.getCanonicalName());
        }
        return layout;
    }

    private ByteBuffer[] getIdentityAlwaysFields() {
        if (identityAlwaysFields == null) {
            int[] indexes = table.getMetadata(PostgresTableMetadata.class).getIdentityAlwaysIndexes();
            identityAlwaysFields = materialize(indexes);
        }
        return identityAlwaysFields;
    }

    private ByteBuffer[] materialize(int[] indexes) {
        if (indexes.length == 0) {
            return EMPTY_FIELDS;
        }
        int maxIndex = 0;
        for (int index : indexes) {
            maxIndex = Math.max(maxIndex, index);
        }
        PostgresTupleLayout current = getLayout();
        current.ensureFields(maxIndex + 1);
        ByteBuffer[] result = new ByteBuffer[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            result[i] = current.getField(indexes[i]);
        }
        return result;
    }
}
