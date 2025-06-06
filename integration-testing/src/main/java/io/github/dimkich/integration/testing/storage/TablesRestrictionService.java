package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.util.CollectionUtils;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class TablesRestrictionService {
    private static final String TABLE_NAME = "tests_allowed_tables";
    private final Connection connection;
    private final String adminUsername;

    private Set<String> tables;
    private Set<String> allowedTables;

    public void setAllowedTables(Set<String> allowedTables) throws SQLException {
        init();
        if (allowedTables == null) {
            allowedTables = new HashSet<>();
        }
        if (this.allowedTables.equals(allowedTables)) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        CollectionUtils.setsDifference(this.allowedTables, allowedTables).forEach(name ->
                builder.append("delete from ").append(TABLE_NAME).append(" where name ='").append(name).append("';\n")
        );
        CollectionUtils.setsDifference(allowedTables, this.allowedTables).forEach(name ->
            builder.append("insert into ").append(TABLE_NAME).append(" values ('").append(name).append("');\n")
        );
        if (!builder.isEmpty()) {
            @Cleanup Statement statement = connection.createStatement();
            statement.execute(builder.toString());
        }
        this.allowedTables = allowedTables;
    }

    private void init() throws SQLException {
        if (tables == null) {
            StringBuilder builder = new StringBuilder("""
                    create table if not exists :tableName
                    (
                      name text not null,
                        constraint :tableName_pk
                            primary key (name)
                    );
                    grant select on :tableName to public;
                    create or replace function testsRestrictedTableChanges()
                        returns trigger
                        language plpgsql
                    as
                    $$
                    DECLARE curr_user TEXT;
                    begin
                        curr_user := (select current_user limit 1);
                        IF curr_user=':adminUsername' THEN
                            return null;
                        END IF;
                        PERFORM * FROM :tableName where name = tg_table_name LIMIT 1;
                        IF NOT FOUND THEN
                            RAISE EXCEPTION 'Changes to table \"%\" restricted, since it was not declared in tests \"init\" section.', tg_table_name;
                        END IF;
                        RETURN NULL;
                    end;
                    $$;
                    """.replace(":tableName", TABLE_NAME)
                    .replace(":adminUsername", adminUsername)
            );

            @Cleanup ResultSet rs = connection.getMetaData().getTables(null, null, null,
                    new String[]{"TABLE"});
            tables = new HashSet<>();
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                tables.add(name);
                builder.append("create trigger ").append(name).append("_tests_restricted\n")
                        .append("before insert or update or truncate or delete on ").append(name).append("\n")
                        .append("for each statement execute function testsRestrictedTableChanges();\n");
            }

            @Cleanup Statement statement = connection.createStatement();
            statement.execute(builder.toString());
            builder = new StringBuilder();
            for (String name : tables) {
                builder.append("insert into ").append(TABLE_NAME).append(" values ('").append(name).append("');\n");
            }
            statement.execute(builder.toString());
            allowedTables = new HashSet<>(tables);
        }
    }
}
