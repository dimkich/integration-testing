Troubleshooting
===============

[Russian version](../../ru/postgresql/Troubleshooting.md)

## The Diff Is Empty Although the Application Changed Data

Check the following:

1. **Connection name.** `name` in `<init>` and the tag inside `<dataStorageDiff>` must match a key in
   `integration.testing.postgresql.connections`.
2. **Same database.** The framework opens its own JDBC connection. If the application writes to a
   different database or schema, the diff sees nothing.
3. **`defaultSchema`.** For unqualified table names, the module looks in `defaultSchema`.
4. **Triggers.** Changes made under `session_replication_role = 'replica'` or with disabled triggers
   are not captured.
5. **Initial state.** The diff is relative to the previous case or part, not to an empty database.

If there are no errors and the diff is still empty, enable framework debug logs and verify that the
table is part of the initialization.

## `PostgreSQL connection '...' is not configured`

The configuration does not contain a connection with that name. Check the key in
`integration.testing.postgresql.connections` and `name` in XML. Remember that the connection name is
not necessarily an application `DataSource` bean name.

## `TestDataStorage '...' not found`

`SqlStorageSetup`/`SqlStorageInit` references an unknown storage. Make sure that:

* the connection is declared in `connections`;
* `@EnableTestPostgreSQL` is present on the test class;
* the name in XML matches the connection key.

## `Table '...' is not registered in the storage`

The table was not found in the metadata. Usually the cause is one of these:

* the table has the wrong schema (`public.users` instead of `users`, or vice versa);
* `defaultSchema` does not match the schema where the table actually lives.

Check `defaultSchema` and the table names in `tablesToClear`/`tablesToLoad`.

## Permission Errors

If test startup fails while creating the journal, functions, or triggers, the account lacks
privileges. It needs permission to:

* create a table, sequence, functions, and triggers in the schema;
* create and drop triggers on the tested tables;
* execute `SET session_replication_role`;
* execute `ALTER SEQUENCE ... RESTART`;
* read from and write to the service journal.

In a test environment, the simplest option is to use the schema owner or a dedicated role with
explicit grants. If `SET session_replication_role` is forbidden, the module cannot load fixtures or
execute service SQL transitions.

## `bytea` Is Displayed as Text

Check the PostgreSQL setting:

```sql
SHOW bytea_output;
```

The module expects `hex`. With `escape`, binary values may appear as text instead of Base64 `byte[]`.

## Column Conversion Error

A message such as:

```text
Failed to convert PostgreSQL column public.events.payload of type jsonb
```

means that the value could not be converted according to the `columns` setting. Check:

* `type`/`bean-ref` for the column;
* `target-class` when `json` is used;
* whether the JSON structure really matches the expected DTO.

If a custom converter is configured, there is no built-in fallback. Fix the configuration or the
converter itself.

## Table Without a Primary Key

If a table has no primary key, the row key in the diff is a hash of the entire row content. An update
appears as a deleted old row and an added new row. This is expected behavior.

Add a primary key if you need stable keys and convenient reverts. For a composite key, prefer columns
with simple values.

## `hstore` Does Not Work

Make sure the extension is installed in the database:

```sql
CREATE EXTENSION IF NOT EXISTS hstore;
```

Without the extension, PostgreSQL cannot create an `hstore` column and the test fails at the schema
stage.

## No `change` Attribute in the Diff

Check the diff shape setting:

```yaml
integration:
  testing:
    storage:
      storages:
        dataSource:
          keyType: [ STRING, OBJECT ]
          valueType: [ OBJECT, OBJECT ]
          changeType: [ false, true ]
```

Without `changeType`, records may be rendered without `change="added|changed|deleted"`.

## DBUnit Data Was Not Loaded

`SqlStorageSetup` only registers the dataset. To load data into a table, use `SqlStorageInit`:

```xml
<init type="SqlStorageInit" name="dataSource">
    <tablesToLoad>users</tablesToLoad>
</init>
```

or `loadAllTables="true"`. Also check that the table exists in the dataset and the name matches.

## DDL Was Not Picked Up

Run schema and structure changes in `SqlStorageSetup`. After SQL changes, the module invalidates the
metadata cache. DDL executed elsewhere in the test may not refresh the table metadata.

## Testcontainers / Docker Errors

With embedded mode, make sure Docker is running and accessible. For an external database, disable
embedded mode and set `host`, `port`, `database`, `user`, and `password` explicitly.

---
[← Back to documentation](../README.md)
