package io.github.dimkich.integration.testing.postgresql.model.revert;

/**
 * One SQL command of a revert batch. Each implementation owns the rendering of its command;
 * command order and assembly stay in the revert renderer.
 */
public interface PostgresRevertStatement {

    String toSql();
}
