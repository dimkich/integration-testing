package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.dbunit.HumanReadableXmlDataSet;
import io.github.dimkich.integration.testing.execution.MockAnswer;
import io.github.dimkich.integration.testing.initialization.SqlStorageInit;
import io.github.dimkich.integration.testing.initialization.SqlStorageSetup;
import io.github.dimkich.integration.testing.storage.sql.state.TablesActionVisitor;
import io.github.dimkich.integration.testing.storage.sql.state.TestStorageStates;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SQLDataStorageService implements TestDataStorage {
    private final SQLDataStorage storage;
    private final BeanFactory beanFactory;

    private final TestStorageStates testStorageStates = new TestStorageStates();
    private final TablesActionVisitor visitor = new TablesActionVisitor();
    private final Set<String> allowedTables = new TreeSet<>();
    private IDataSet dataSet;
    @Getter
    private Map<String, SqlStorageSetup.TableHook> tableHooks = Map.of();
    private boolean initialized = false;
    @Getter
    private Set<String> tables;

    @Override
    public String getName() {
        return storage.getName();
    }

    public void init() throws Exception {
        if (!initialized) {
            tables = storage.initTableRestriction();
            initialized = true;
        }
        testStorageStates.init(this);
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

    public void addInit(SqlStorageInit init) throws Exception {
        testStorageStates.add(this, init);
    }

    public boolean applyChanges() throws Exception {
        testStorageStates.apply(visitor);
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
                storage.loadDataset(new FilteredDataSet(visitor.getTablesToLoad().toArray(new String[0]), dataSet));
            }
            if (!visitor.getSqls().isEmpty()) {
                storage.executeSql(visitor.getSqls());
            }
            if (!visitor.getHooks().isEmpty()) {
                for (SqlStorageSetup.TableHook hook : visitor.getHooks()) {
                    Object bean = beanFactory.getBean(hook.getBeanName());
                    bean.getClass().getMethod(hook.getBeanMethod()).invoke(bean);
                }
            }
            if (!visitor.getNoHookSqls().isEmpty()) {
                storage.executeSql(visitor.getNoHookSqls());
            }
        });
        visitor.clear();
        return true;
    }

    @Override
    @SneakyThrows
    public Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) {
        return storage.getTablesData(allowedTables, excludedFields);
    }

    @Override
    public void setDiff(Map<String, Object> diff) {
        testStorageStates.setDirtyTables(allowedTables);
    }
}
