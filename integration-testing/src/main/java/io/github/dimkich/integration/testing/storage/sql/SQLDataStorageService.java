package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.dbunit.HumanReadableXmlDataSet;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.initialization.SqlStorageSetup;
import io.github.dimkich.integration.testing.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SQLDataStorageService implements TestDataStorage {
    private final SQLDataStorage storage;
    private final BeanFactory beanFactory;
    private final TestExecutor testExecutor;

    private Set<String> allowedTables = Set.of();
    private IDataSet dataSet;
    private Map<String, SqlStorageSetup.TableHook> tableHooks = Map.of();
    private boolean initialized = false;

    @Override
    public String getName() {
        return storage.getName();
    }

    public void executeSqls(Collection<String> sqls) throws Exception {
        if (sqls != null && !sqls.isEmpty()) {
            storage.executeSql(sqls);
        }
    }

    public void setDbUnitXml(Collection<String> paths) throws DataSetException, IOException {
        List<IDataSet> dataSets = new ArrayList<>();
        for (String dataSetPath : paths) {
            IDataSet dataSet = new HumanReadableXmlDataSet(new ClassPathResource(dataSetPath).getInputStream());
            dataSets.add(dataSet);
        }
        this.dataSet = new CompositeDataSet(dataSets.toArray(IDataSet[]::new));
    }

    public void setTableHooks(Collection<SqlStorageSetup.TableHook> tableHooks) {
        this.tableHooks = tableHooks.stream()
                .collect(Collectors.toMap(SqlStorageSetup.TableHook::getTableName, Function.identity()));
    }

    public void prepareData(Collection<String> sqls, Set<String> tablesToChange,
                            Collection<String> tablesToLoad, boolean loadAllTables, boolean disableTableHooks) throws Exception {
        if (!initialized) {
            storage.initTableRestriction();
            initialized = true;
        }
        boolean reloadAll = loadAllTables || sqls != null && !sqls.isEmpty();
        Set<String> changedTables = new HashSet<>();
        if (!reloadAll && tablesToChange != null) {
            changedTables.addAll(tablesToChange);
        }

        if (tablesToChange != null && !tablesToChange.equals(allowedTables)) {
//            storage.clearTables();
            TableRestrictionBuilder builder = storage.getRestrictionBuilder();
            for (String table : CollectionUtils.setsDifference(allowedTables, tablesToChange).toList()) {
                builder.restrictTable(table);
                if (!reloadAll) {
                    changedTables.add(table);
                }
            }
            for (String table : CollectionUtils.setsDifference(tablesToChange, allowedTables).toList()) {
                builder.allowTable(table);
                if (!reloadAll) {
                    changedTables.add(table);
                }
            }
            builder.finish();
            allowedTables = tablesToChange;
            storage.setTablesToClear(allowedTables);
        }
        if (tablesToChange != null) {
            storage.clearTables();
        }

        if (sqls != null && !sqls.isEmpty()) {
            storage.executeSql(sqls);
        }

        if (loadAllTables) {
            storage.loadDataset(dataSet);
        } else if (tablesToLoad != null) {
            storage.loadDataset(new FilteredDataSet(tablesToLoad.toArray(new String[0]), dataSet));
            if (!reloadAll) {
                changedTables.addAll(tablesToLoad);
            }
        }

        if (reloadAll) {
            if (!disableTableHooks) {
                for (SqlStorageSetup.TableHook table : tableHooks.values()) {
                    this.reloadCache(table);
                }
            }
        } else if (!changedTables.isEmpty()) {
            if (!disableTableHooks) {
                for (String table : changedTables) {
                    this.reloadCache(table);
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) {
        return storage.getTablesData(allowedTables, excludedFields);
    }

    private void reloadCache(String table) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        SqlStorageSetup.TableHook reload = tableHooks.get(table);
        if (reload != null) {
            reloadCache(reload);
        }
    }

    private void reloadCache(SqlStorageSetup.TableHook reload) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        testExecutor.setExecuting(true);
        try {
            Object bean = beanFactory.getBean(reload.getBeanName());
            bean.getClass().getMethod(reload.getBeanMethod()).invoke(bean);
        } finally {
            testExecutor.setExecuting(false);
        }
    }
}
