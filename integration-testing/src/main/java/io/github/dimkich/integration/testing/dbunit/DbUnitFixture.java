package io.github.dimkich.integration.testing.dbunit;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbunit.dataset.AbstractTable;
import org.dbunit.dataset.AbstractTableMetaData;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.TableDecoratorDataSet;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DBUnit-backed fixture configured for one SQL storage. The fixture is optional: an empty path set
 * leaves it unconfigured and the storage simply has nothing to load.
 * <p>
 * Fixture table names are resolved to canonical {@link Table}s at configuration time. The default
 * schema of the owning {@link TableRegistry} is applied to unqualified names. The wrapped data set
 * exposes the same canonical names, so filtering never has to remember the original DBUnit names.
 */
@Slf4j
@RequiredArgsConstructor
public class DbUnitFixture {
    private final TableRegistry registry;

    private IDataSet dataSet;
    private TableSet tables;
    private TableMap<Integer> rowCounts = new TableMap<>();

    public void setDbUnitXml(Collection<String> paths) throws DataSetException, IOException {
        if (paths == null || paths.isEmpty()) {
            clear();
            return;
        }
        List<IDataSet> dataSets = new ArrayList<>();
        for (String path : paths) {
            @Cleanup InputStream input = new ClassPathResource(path).getInputStream();
            dataSets.add(new HumanReadableXmlDataSet(input));
        }
        IDataSet rawDataSet = new CompositeDataSet(dataSets.toArray(IDataSet[]::new));

        TableSet newTables = TableSet.empty(registry);
        TableMap<Integer> newRowCounts = new TableMap<>();
        Map<String, String> canonicalNames = new HashMap<>();
        for (String rawName : rawDataSet.getTableNames()) {
            Table table = registry.resolveOrRegister(TableName.parse(rawName));
            canonicalNames.put(rawName, table.getCanonicalName());
            int rowCount;
            try {
                rowCount = rawDataSet.getTable(rawName).getRowCount();
            } catch (DataSetException e) {
                log.debug("Failed to read row count of fixture table '{}', using -1", rawName, e);
                rowCount = -1;
            }
            newTables.add(table);
            newRowCounts.put(table, rowCount);
        }
        this.dataSet = new TableDecoratorDataSet(rawDataSet, table -> {
            String rawName = table.getTableMetaData().getTableName();
            return new RenamedTable(table, canonicalNames.get(rawName));
        });
        this.tables = newTables;
        this.rowCounts = newRowCounts;
    }

    public boolean isConfigured() {
        return dataSet != null;
    }

    /**
     * Tables declared by the fixture.
     */
    public TableSet getTables() {
        return tables == null ? TableSet.empty(registry) : tables;
    }

    /**
     * Row count of one table, or {@code -1} when the table is not present in the fixture.
     */
    public int getRowCount(Table table) {
        Integer rowCount = rowCounts.get(table);
        return rowCount == null ? -1 : rowCount;
    }

    /**
     * Returns the DBUnit view of the requested tables. The storage owns the actual DBUnit operation,
     * so this method only filters the already parsed data set.
     */
    public IDataSet filteredDataSet(TableSet tables) throws DataSetException {
        if (dataSet == null) {
            throw new IllegalStateException("DBUnit fixture is not configured");
        }
        String[] names = new String[tables.size()];
        int index = 0;
        for (Table table : tables) {
            names[index++] = table.getCanonicalName();
        }
        return new FilteredDataSet(names, dataSet);
    }

    private void clear() {
        dataSet = null;
        tables = null;
        rowCounts = new TableMap<>();
    }

    private static final class RenamedTable extends AbstractTable {
        private final ITable delegate;
        @Getter
        private final ITableMetaData tableMetaData;

        private RenamedTable(ITable delegate, String tableName) {
            this.delegate = delegate;
            this.tableMetaData = new RenamedTableMetaData(delegate.getTableMetaData(), tableName);
        }

        @Override
        public int getRowCount() {
            return delegate.getRowCount();
        }

        @Override
        public Object getValue(int row, String column) throws DataSetException {
            return delegate.getValue(row, column);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class RenamedTableMetaData extends AbstractTableMetaData {
        private final ITableMetaData delegate;
        @Getter
        private final String tableName;

        @Override
        public Column[] getColumns() throws DataSetException {
            return delegate.getColumns();
        }

        @Override
        public Column[] getPrimaryKeys() throws DataSetException {
            return delegate.getPrimaryKeys();
        }
    }
}
