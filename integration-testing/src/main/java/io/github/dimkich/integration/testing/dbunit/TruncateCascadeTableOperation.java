package io.github.dimkich.integration.testing.dbunit;

public class TruncateCascadeTableOperation extends BatchTableOperation {
    @Override
    protected String createSql(String qualifiedName) {
        return "truncate table " + qualifiedName + " cascade";
    }
}
