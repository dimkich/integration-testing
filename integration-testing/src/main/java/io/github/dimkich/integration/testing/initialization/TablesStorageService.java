package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.dbunit.CustomPostgresqlDataTypeFactory;
import io.github.dimkich.integration.testing.dbunit.DisableTriggersOperation;
import io.github.dimkich.integration.testing.dbunit.HumanReadableXmlDataSet;
import io.github.dimkich.integration.testing.dbunit.TruncateCascadeTableOperation;
import io.github.dimkich.integration.testing.storage.DataSourceStorage;
import io.github.dimkich.integration.testing.util.FunctionWithIO;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.CompositeOperation;
import org.dbunit.operation.DatabaseOperation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TablesStorageService {
    private static final DatabaseOperation TRUNCATE_CASCADE_TABLE = new TruncateCascadeTableOperation();
    private static final DatabaseOperation CLEAN_INSERT = new CompositeOperation(TRUNCATE_CASCADE_TABLE, DatabaseOperation.INSERT);

    private final BeanFactory beanFactory;

    private final Map<String, TablesStorageSetup.TableCacheReload> cacheReloadMap = new HashMap<>();
    private IDatabaseConnection dbUnitConnection;
    private IDataSet dataSet;

    public void addCacheReload(TablesStorageSetup.TableCacheReload cacheReload) {
        cacheReloadMap.put(cacheReload.getTableName(), cacheReload);
    }

    public void addDbUnitXml(List<String> paths) throws IOException, DataSetException {
        List<IDataSet> dataSets = new ArrayList<>();
        for (String dataSetPath : paths) {
            IDataSet dataSet = new HumanReadableXmlDataSet(new ClassPathResource(dataSetPath).getInputStream());
            dataSets.add(dataSet);
        }
        dataSet = new CompositeDataSet(dataSets.toArray(IDataSet[]::new));
    }

    public void executeSqls(Connection connection, List<String> sqls, FunctionWithIO<String, String> convert) throws SQLException, IOException {
        if (sqls != null) {
            for (String sql : sqls) {
                @Cleanup Statement statement = connection.createStatement();
                statement.execute(convert.apply(sql));
            }
        }
    }

    public void clearTables(DataSourceStorage storage, Set<String> tables) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, SQLException {
        storage.setTableNames(tables);
        storage.clear();
        reloadTablesCache(tables);
    }

    public void loadTablesData(DataSourceStorage storage, Collection<String> tables) throws Exception {
        IDataSet filtered = new FilteredDataSet(tables.toArray(new String[0]), dataSet);
        new DisableTriggersOperation(CLEAN_INSERT).execute(getDbUnitConnection(storage), filtered);
        reloadTablesCache(tables);
    }

    public void loadAllTablesData(DataSourceStorage storage) throws Exception {
        new DisableTriggersOperation(CLEAN_INSERT).execute(getDbUnitConnection(storage), dataSet);
        reloadTablesCache(cacheReloadMap.keySet());
    }

    protected IDatabaseConnection getDbUnitConnection(DataSourceStorage storage) throws DatabaseUnitException {
        if (dbUnitConnection == null) {
            dbUnitConnection = new DatabaseConnection(storage.getConnection(), null);
            dbUnitConnection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                    new CustomPostgresqlDataTypeFactory());
            dbUnitConnection.getConfig().setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
        }
        return dbUnitConnection;
    }

    private void reloadTablesCache(Collection<String> tables) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for (String table : tables) {
            TablesStorageSetup.TableCacheReload reload = cacheReloadMap.get(table);
            if (reload != null) {
                Object bean = beanFactory.getBean(reload.getBeanName());
                bean.getClass().getMethod(reload.getBeanMethod()).invoke(bean);
            }
        }
    }
}
