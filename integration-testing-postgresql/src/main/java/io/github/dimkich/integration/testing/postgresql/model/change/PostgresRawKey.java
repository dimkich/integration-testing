package io.github.dimkich.integration.testing.postgresql.model.change;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Identity of a raw row inside its table state.
 * <p>
 * A key contains either the raw primary-key fields or, for a table without a primary key, the
 * complete raw tuple. The fields are views into the tuple buffer, so key creation does not copy
 * row data. Two keys are equal when they have the same table and the same identity fields. The
 * hash is cached lazily; the SHA-256 digest is computed only for the display key of a table
 * without a primary key.
 * <p>
 * The key is immutable and content-based. It stores a table reference so that it can also render
 * the display key used by the difference engine and test XML. The key is intended to live together
 * with the raw row and table state in one change batch and is not thread-safe.
 */
@EqualsAndHashCode(of = {"table", "primaryKey", "keyFields"},
        cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class PostgresRawKey {
    private static final HexFormat HEX = HexFormat.of();
    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    });

    private final Table table;
    private final boolean primaryKey;
    private final ByteBuffer[] keyFields;
    private byte[] tupleHash;

    /**
     * Display key of the row in the difference engine and test XML. A primary-key row is rendered
     * from the converted values; a table without a primary key uses the raw tuple hash.
     */
    public String getDisplayKey(Map<String, ?> values) {
        if (!primaryKey) {
            return table.getDisplayName() + "_" + HEX.formatHex(tupleHash());
        }
        List<String> primaryKey = table.getMetadata(PostgresTableMetadata.class).getPrimaryKey();
        StringBuilder builder = new StringBuilder(table.getDisplayName().length()
                + primaryKey.size() * 16);
        builder.append(table.getDisplayName());
        for (String column : primaryKey) {
            builder.append('_').append(values.get(column));
        }
        return builder.toString();
    }

    private byte[] tupleHash() {
        if (tupleHash == null) {
            MessageDigest digest = SHA_256.get();
            digest.reset();
            digest.update(keyFields[0].duplicate());
            tupleHash = digest.digest();
        }
        return tupleHash;
    }
}
