package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageInit;
import io.github.dimkich.integration.testing.storage.sql.state.StorageState;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the initial {@link StorageState} of a storage: the default one or the one declared by an
 * {@link SqlStorageInit}.
 */
@RequiredArgsConstructor
public final class StorageStateFactory {
    private final TableRegistry registry;
    private final DbUnitFixture fixture;

    /**
     * Baseline state of the storage: every current table is declared cleared.
     */
    public StorageState createDefault() {
        StorageState state = new StorageState(registry);
        for (Table table : registry.getCurrentTables()) {
            state.setBaselineCleared(table);
        }
        return state;
    }

    /**
     * State built from an init declaration: tables of the clear block are declared cleared, of the
     * load block — loaded; SQL blocks are stored in the global mutation log in declaration order.
     */
    public StorageState createFromInit(SqlStorageInit init) {
        StorageState state = new StorageState(registry);

        Set<TableName> toClear = parseCommaSeparatedNames(init.getTablesToClear());
        for (TableName table : toClear) {
            state.setBaselineCleared(registry.resolve(table));
        }

        boolean loadAll = Boolean.TRUE.equals(init.getLoadAllTables());
        if (fixture.isConfigured()) {
            if (loadAll) {
                for (Table table : fixture.getTables()) {
                    state.setBaselineLoaded(table);
                }
            } else {
                for (TableName table : parseCommaSeparatedNames(init.getTablesToLoad())) {
                    state.setBaselineLoaded(registry.resolve(table));
                }
            }
        }

        if (init.getSql() != null) {
            for (String sql : init.getSql()) {
                state.addInitSql(sql);
            }
        }
        return state;
    }

    private static Set<TableName> parseCommaSeparatedNames(String string) {
        if (!StringUtils.hasText(string)) {
            return Set.of();
        }
        return Arrays.stream(string.split(","))
                .map(String::strip)
                .filter(StringUtils::hasText)
                .map(TableName::parse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
