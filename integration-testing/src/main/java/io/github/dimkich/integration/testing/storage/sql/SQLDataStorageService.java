package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.dbunit.HumanReadableXmlDataSet;
import io.github.dimkich.integration.testing.execution.MockAnswer;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.initialization.bean.BeanInit;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageSetup;
import io.github.dimkich.integration.testing.storage.sql.state.TableStates;
import io.github.dimkich.integration.testing.storage.sql.state.TablesActionVisitor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SQLDataStorageService implements TestDataStorage {
    private final SQLDataStorage storage;
    private final InitializationService initializationService;

    private final TablesActionVisitor visitor = new TablesActionVisitor();
    private final Set<String> allowedTables = new TreeSet<>();
    private IDataSet dataSet;
    @Getter
    private Map<String, List<SqlStorageSetup.TableHook>> tableHooks = Map.of();
    @Getter
    private Set<String> tables = Set.of();
    private TableStates currentState;

    @Override
    public String getName() {
        return storage.getName();
    }

    public void init() throws Exception {
        Set<String> newTables = storage.getTables();
        List<String> toRestrict = newTables.stream()
                .filter(t -> !tables.contains(t))
                .toList();
        if (!toRestrict.isEmpty()) {
            storage.initTablesRestriction(toRestrict);
        }
        tables = newTables;
    }

    public void executeSqls(Collection<String> sqls) throws Exception {
        if (sqls != null && !sqls.isEmpty()) {
            storage.executeSql(sqls);
        }
    }

    public Collection<String> getLoadableTables() throws DataSetException {
        if (dataSet == null) {
            return List.of();
        }
        return Arrays.asList(dataSet.getTableNames());
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
                .collect(Collectors.groupingBy(SqlStorageSetup.TableHook::getTableName));
    }

    public boolean applyChanges(TableStates oldState, TableStates newState, boolean checkDirty) throws Exception {
        visitor.setCheckDirty(checkDirty);
        oldState.compare(newState, visitor);
        currentState = newState;

        if (!visitor.isAnyChanges()) {
            return false;
        }
        if (!visitor.getTablesToRestartIdentity().isEmpty()) {
            visitor.getSqls().addFirst(storage.getRestartIdentitySql(visitor.getTablesToRestartIdentity()));
        }
        if (!visitor.getTablesToClear().isEmpty()) {
            visitor.getSqls().addFirst(storage.getClearSql(visitor.getTablesToClear()));
        }
        if (!visitor.getTablesToDeny().isEmpty() || !visitor.getTablesToAllow().isEmpty()) {
            for (String table : visitor.getTablesToDeny()) {
                allowedTables.remove(table);
                visitor.getSqls().addFirst(storage.getRestrictTableSql(table));
            }
            for (String table : visitor.getTablesToAllow()) {
                allowedTables.add(table);
                visitor.getSqls().addFirst(storage.getAllowTableSql(table));
            }
        }
        MockAnswer.enable(() -> {
            if (!visitor.getTablesToLoad().isEmpty()) {
                log.debug("Init '{}' load tables: {}", storage.getName(), visitor.getTablesToLoad());
                storage.loadDataset(new FilteredDataSet(visitor.getTablesToLoad().toArray(new String[0]), dataSet));
            }
            if (!visitor.getSqls().isEmpty()) {
                log.debug("Init '{}' SQL: {}", storage.getName(), visitor.getSqls());
                storage.executeSql(visitor.getSqls());
            }
            if (!visitor.getHooks().isEmpty()) {
                log.debug("Init '{}' table hooks: {}", storage.getName(), visitor.getHooks());
                List<BeanInit.BeanMethod> list = visitor.getHooks().stream()
                        .map(th -> {
                            BeanInit.BeanMethod beanMethod = new BeanInit.BeanMethod();
                            beanMethod.setName(th.getBeanName());
                            beanMethod.setMethod(th.getBeanMethod());
                            return beanMethod;
                        }).toList();
                BeanInit beanInit = new BeanInit();
                beanInit.setBean(list);
                initializationService.addTransientInit(beanInit);
            }
            if (!visitor.getNoHookSqls().isEmpty()) {
                log.debug("Init '{}' no hook SQL: {}", storage.getName(), visitor.getNoHookSqls());
                storage.executeSql(visitor.getNoHookSqls());
            }
        });
        visitor.clear();
        return true;
    }

    public void clearTables(Collection<String> tableNames) throws Exception {
        storage.executeSql(List.of(storage.getRestartIdentitySql(tableNames), storage.getClearSql(tableNames)));
    }

    @Override
    @SneakyThrows
    public Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) {
        return storage.getTablesData(allowedTables, excludedFields);
    }

    @Override
    public void setDiff(Map<String, Object> diff) {
        if (currentState != null) {
            currentState.setDirtyTables(allowedTables);
        }
    }
}
