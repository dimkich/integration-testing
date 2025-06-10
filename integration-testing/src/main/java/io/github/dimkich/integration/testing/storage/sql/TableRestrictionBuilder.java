package io.github.dimkich.integration.testing.storage.sql;

public interface TableRestrictionBuilder {
    void allowTable(String table) throws Exception;

    void restrictTable(String table) throws Exception;

    void finish() throws Exception;
}
