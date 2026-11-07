package io.github.dimkich.integration.testing.initialization.sql;

import eu.ciechanowiec.sneakyfun.SneakyBiConsumer;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class SqlStorageSetupSetup implements InitSetup<SqlStorageSetup, SqlStorageSetupState> {
    private final TestDataStorages testDataStorages;

    @Override
    public Class<SqlStorageSetup> getTestCaseInitClass() {
        return SqlStorageSetup.class;
    }

    @Override
    public SqlStorageSetupState defaultState() {
        return new SqlStorageSetupState();
    }

    @Override
    public SqlStorageSetupState convert(SqlStorageSetup init) throws IOException {
        SqlStorageSetupState.InitData data = new SqlStorageSetupState.InitData();
        if (init.getSqlFilePath() != null) {
            for (String sqlFile : init.getSqlFilePath()) {
                @Cleanup InputStream input = new ClassPathResource(sqlFile).getInputStream();
                data.getSql().add(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        if (init.getSql() != null) {
            data.getSql().addAll(init.getSql());
        }
        if (init.getDbUnitPath() != null) {
            data.getDbUnitPath().addAll(init.getDbUnitPath());
        }
        SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(),
                SQLDataStorageService.class);
        return new SqlStorageSetupState(storage, data);
    }

    @Override
    public void apply(SqlStorageSetupState oldState, SqlStorageSetupState newState, Test test) {
        newState.getInitData().forEach(SneakyBiConsumer.sneaky((s, newData) -> {
            SqlStorageSetupState.InitData oldData = oldState.getInitData(s);
            if (!newData.getSql().isEmpty() && (oldData == null || !oldData.getSql().equals(newData.getSql()))) {
                log.debug("Sql setup '{}', sqls {}", s.getName(), newData.getSql());
                s.executeSqls(newData.getSql());
            }
            if (oldData == null || !oldData.getDbUnitPath().equals(newData.getDbUnitPath())) {
                log.debug("Sql setup '{}', dbunit {}", s.getName(), newData.getDbUnitPath());
                s.setDbUnitXml(newData.getDbUnitPath());
            }
            s.init();
        }));
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}
