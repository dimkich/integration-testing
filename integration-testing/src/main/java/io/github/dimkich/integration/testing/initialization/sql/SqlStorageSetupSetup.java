package io.github.dimkich.integration.testing.initialization.sql;

import eu.ciechanowiec.sneakyfun.SneakyBiConsumer;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
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
                data.getSql().add(new String(new ClassPathResource(sqlFile).getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8));
            }
        }
        if (init.getSql() != null) {
            data.getSql().addAll(init.getSql());
        }
        if (init.getDbUnitPath() != null) {
            data.getDbUnitPath().addAll(init.getDbUnitPath());
        }
        if (init.getTableHook() != null) {
            data.getTableHook().addAll(init.getTableHook());
        }
        SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(),
                SQLDataStorageService.class);
        return new SqlStorageSetupState(storage, data);
    }

    @Override
    public void apply(SqlStorageSetupState oldState, SqlStorageSetupState newState, Test test) throws Exception {
        newState.getInitData().forEach(SneakyBiConsumer.sneaky((s, data) -> {
            if (!data.getSql().isEmpty()) {
                log.debug("Slq setup '{}', sqls {}", s.getName(), data.getSql());
                s.executeSqls(data.getSql());
                testDataStorages.addAffectedStorage(s);
            }
            log.debug("Slq setup '{}', dbunit {}", s.getName(), data.getDbUnitPath());
            s.setDbUnitXml(data.getDbUnitPath());
            log.debug("Slq setup '{}', table hooks {}", s.getName(), data.getTableHook());
            s.setTableHooks(data.getTableHook());
            s.init();
        }));
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}
