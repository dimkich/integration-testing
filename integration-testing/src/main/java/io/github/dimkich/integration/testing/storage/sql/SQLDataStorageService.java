package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.dbunit.HumanReadableXmlDataSet;
import io.github.dimkich.integration.testing.initialization.TablesStorageSetup;
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

    private Set<String> allowedTables = Set.of();
    private IDataSet dataSet;
    private Map<String, TablesStorageSetup.TableCacheReload> tableCacheReload = Map.of();
    private boolean initialized = false;

    @Override
    public String getName() {
        return storage.getName();
    }

    public void setDbUnitXml(List<String> paths) throws DataSetException, IOException {
        List<IDataSet> dataSets = new ArrayList<>();
        for (String dataSetPath : paths) {
            IDataSet dataSet = new HumanReadableXmlDataSet(new ClassPathResource(dataSetPath).getInputStream());
            dataSets.add(dataSet);
        }
        this.dataSet = new CompositeDataSet(dataSets.toArray(IDataSet[]::new));
    }

    public void setTableCacheReload(List<TablesStorageSetup.TableCacheReload> tableCacheReload) {
        this.tableCacheReload = tableCacheReload.stream()
                .collect(Collectors.toMap(TablesStorageSetup.TableCacheReload::getTableName, Function.identity()));
    }

    public void prepareData(Collection<String> sqls, Set<String> tablesToChange,
                            Collection<String> tablesToLoad, boolean loadAllTables, boolean disableCacheReload) throws Exception {
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
            if (!disableCacheReload) {
                for (TablesStorageSetup.TableCacheReload table : tableCacheReload.values()) {
                    this.reloadCache(table);
                }
            }
        } else if (!changedTables.isEmpty()) {
            if (!disableCacheReload) {
                for (String table : changedTables) {
                    this.reloadCache(table);
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public Map<Object, Object> getCurrentValue() {
        return storage.getTablesData(allowedTables).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k2, LinkedHashMap::new));
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void clear() {
    }

    private void reloadCache(String table) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TablesStorageSetup.TableCacheReload reload = tableCacheReload.get(table);
        if (reload != null) {
            reloadCache(reload);
        }
    }

    private void reloadCache(TablesStorageSetup.TableCacheReload reload) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        Object bean = beanFactory.getBean(reload.getBeanName());
        bean.getClass().getMethod(reload.getBeanMethod()).invoke(bean);
    }
}
