package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Application-side hook used by the test XML to emulate the infrastructure scenario: a persistent
 * component changes a table during the initialization phase (the triggers capture the change as an
 * untagged record) and then the harness resets the tables via
 * {@link SQLDataStorageService#clearTables(java.util.Collection)}, which must forget these records.
 */
@RequiredArgsConstructor
public class ClearTablesHook {
    private final JdbcTemplate jdbcTemplate;
    private final TestDataStorages testDataStorages;

    public void insertAndClearUsers() throws Exception {
        jdbcTemplate.execute("INSERT INTO users (id, name, email) VALUES (9, 'External', 'external@test.com')");
        testDataStorages.getTestDataStorage("dataSource", SQLDataStorageService.class).clearTables(List.of("users"));
    }
}
