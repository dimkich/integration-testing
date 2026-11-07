package io.github.dimkich.integration.testing.dbunit;

public class DeleteFromTableOperation extends BatchTableOperation {
    @Override
    protected String createSql(String qualifiedName) {
        return "delete from " + qualifiedName;
    }
}
