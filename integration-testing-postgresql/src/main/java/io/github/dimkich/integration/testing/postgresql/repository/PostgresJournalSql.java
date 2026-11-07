package io.github.dimkich.integration.testing.postgresql.repository;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.experimental.UtilityClass;

/**
 * SQL used by the audit journal repository. Keeping the DDL and trigger definitions here leaves
 * {@link PostgresJournalRepository} responsible only for JDBC execution.
 */
@UtilityClass
class PostgresJournalSql {
    private static final String JOURNAL_TABLE = PostgresInternalTableNames.AUDIT_JOURNAL;

    static String initializationSql() {
        return """
                CREATE UNLOGGED TABLE IF NOT EXISTS %1$s
                (
                    id         BIGSERIAL PRIMARY KEY,
                    table_name TEXT NOT NULL,
                    action     CHAR(1) NOT NULL,
                    old_data   TEXT,
                    new_data   TEXT,
                    node       INTEGER
                );
                GRANT INSERT, SELECT ON %1$s TO PUBLIC;
                GRANT USAGE, SELECT ON SEQUENCE %1$s_id_seq TO PUBLIC;
                CREATE OR REPLACE FUNCTION _it_audit_row_trigger()
                    RETURNS TRIGGER
                    LANGUAGE plpgsql
                AS
                $$
                DECLARE
                    node_tag INTEGER := NULLIF(current_setting('it.node', true), '')::INTEGER;
                BEGIN
                    IF TG_OP = 'INSERT' THEN
                        INSERT INTO %1$s (table_name, action, new_data, node)
                        VALUES (TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME, 'I', NEW::text, node_tag);
                    ELSIF TG_OP = 'UPDATE' THEN
                        INSERT INTO %1$s (table_name, action, old_data, new_data, node)
                        VALUES (TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME, 'U', OLD::text, NEW::text, node_tag);
                    ELSIF TG_OP = 'DELETE' THEN
                        INSERT INTO %1$s (table_name, action, old_data, node)
                        VALUES (TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME, 'D', OLD::text, node_tag);
                    END IF;
                    RETURN NULL;
                END;
                $$;
                CREATE OR REPLACE FUNCTION _it_audit_truncate_trigger()
                    RETURNS TRIGGER
                    LANGUAGE plpgsql
                AS
                $$
                DECLARE
                    full_name TEXT := TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME;
                    node_tag INTEGER := NULLIF(current_setting('it.node', true), '')::INTEGER;
                BEGIN
                    EXECUTE format(
                        'INSERT INTO %1$s (table_name, action, old_data, node)
                         SELECT %%L, ''D'', t::text, %%L FROM %%I.%%I t',
                        full_name, node_tag, TG_TABLE_SCHEMA, TG_TABLE_NAME);
                    RETURN NULL;
                END;
                $$;
                """.formatted(JOURNAL_TABLE);
    }

    static String attachTriggersSql(TableSet requestedTables) {
        StringBuilder sql = new StringBuilder(Math.max(128, requestedTables.size() * 384));
        for (Table table : requestedTables) {
            String qualified = table.getQualified();
            sql.append("DROP TRIGGER IF EXISTS _it_audit_row ON ").append(qualified).append(";\n")
                    .append("DROP TRIGGER IF EXISTS _it_audit_truncate ON ").append(qualified).append(";\n")
                    .append("CREATE TRIGGER _it_audit_row AFTER INSERT OR UPDATE OR DELETE ON ")
                    .append(qualified).append(" FOR EACH ROW EXECUTE FUNCTION _it_audit_row_trigger();\n")
                    .append("CREATE TRIGGER _it_audit_truncate BEFORE TRUNCATE ON ")
                    .append(qualified).append(" FOR EACH STATEMENT EXECUTE FUNCTION _it_audit_truncate_trigger();\n");
        }
        return sql.toString();
    }

    static String truncateSql() {
        return "TRUNCATE " + JOURNAL_TABLE;
    }

    static String selectAfter(long lastReadId) {
        return "SELECT id, table_name, action, old_data, new_data, node FROM "
                + JOURNAL_TABLE + " WHERE id > " + lastReadId + " ORDER BY id ASC";
    }
}
