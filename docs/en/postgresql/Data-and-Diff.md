Data and Diff
=============

[Russian version](../../ru/postgresql/Data-and-Diff.md)

A PostgreSQL test usually has two parts:

1. **Preparation.** In `<init>` you create the schema, load a DBUnit dataset, and optionally run SQL.
2. **Verification.** In `<dataStorageDiff>` you describe which rows and columns should change after the
   application code runs.

## SqlStorageSetup

`SqlStorageSetup` configures the storage once per container or test class. It runs SQL files, inline
SQL, and registers a DBUnit dataset.

```xml
<init type="SqlStorageSetup" name="dataSource">
    <sqlFilePath>schema.sql</sqlFilePath>
    <sql>ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255)</sql>
    <dbUnitPath>db-data.xml</dbUnitPath>
</init>
```

| Element       | Description                                                       |
|---------------|-------------------------------------------------------------------|
| `name`        | Connection name from `integration.testing.postgresql.connections` |
| `sqlFilePath` | Classpath SQL file paths; executed in order                       |
| `sql`         | Inline SQL; executed after `sqlFilePath`                          |
| `dbUnitPath`  | Classpath DBUnit dataset paths                                    |

`dbUnitPath` only registers the dataset. Data is loaded later by `SqlStorageInit` with `tablesToLoad`
or `loadAllTables="true"`.

Run DDL in `SqlStorageSetup`: when the SQL changes, the module invalidates the metadata cache and
re-reads the table structure.

## SqlStorageInit

`SqlStorageInit` prepares the state before a specific case or part.

```xml
<!-- Load the users table from the DBUnit dataset -->
<init type="SqlStorageInit" name="dataSource">
    <tablesToLoad>users</tablesToLoad>
</init>

<!-- Clear orders and load users -->
<init type="SqlStorageInit" name="dataSource">
    <tablesToClear>orders</tablesToClear>
    <tablesToLoad>users</tablesToLoad>
</init>

<!-- Load all dataset tables -->
<init type="SqlStorageInit" name="dataSource" loadAllTables="true"/>

<!-- Run SQL after loading the data -->
<init type="SqlStorageInit" name="dataSource">
    <tablesToLoad>users</tablesToLoad>
    <sql>UPDATE users SET name = 'Init' WHERE id = 1</sql>
</init>
```

| Attribute/Element | Description                                        |
|-------------------|----------------------------------------------------|
| `name`            | Connection name                                    |
| `tablesToClear`   | Tables to clear                                    |
| `tablesToLoad`    | Tables to load from the DBUnit dataset             |
| `loadAllTables`   | `true` loads every dataset table                   |
| `sql`             | SQL statements executed after clearing and loading |

If a table is listed in both `tablesToClear` and `tablesToLoad`, it is loaded from the dataset.
`tablesToClear` without `tablesToLoad` leaves the table empty.

Table lists are comma-separated. Use fully qualified names (`public.users`) when the same table name
exists in different schemas.

## DBUnit Dataset

A DBUnit file is a regular XML document with a `<dataset>` root. Each table is a tag, and each row is a
repetition of that tag:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataset>
    <users>
        <id>1</id>
        <name>Alice</name>
        <email>alice@example.com</email>
    </users>
    <users>
        <id>2</id>
        <name>Bob</name>
        <email>bob@example.com</email>
    </users>

    <documents>
        <id>100</id>
        <status>NEW</status>
        <!-- NULL in the database; an empty tag would mean an empty string. -->
        <content null="true"/>
    </documents>
</dataset>
```

Column values are written as PostgreSQL literals:

```xml
<dataset>
    <!-- Array -->
    <advanced>
        <id>500</id>
        <tags>{tag_x,tag_y}</tags>
    </advanced>

    <!-- JSON -->
    <advanced>
        <id>501</id>
        <data>{"version": 5, "active": true}</data>
    </advanced>

    <!-- hstore -->
    <datatypes>
        <id>1</id>
        <hs>"k1"=>"v1, with comma", "k2"=>"a:b:c"</hs>
    </datatypes>

    <!-- timestamptz with an explicit offset -->
    <datatypes>
        <id>2</id>
        <ts_tz>2026-01-01 10:15:30.123456+03:00</ts_tz>
    </datatypes>
</dataset>
```

Notes:

* `null="true"` sets SQL `NULL`; an empty tag is an empty string.
* Field order inside a row does not matter. A table's columns are the union of all fields across all
  rows in first-appearance order; a field omitted in a row is `NULL`.
* A duplicate field in one row is an error.
* Arrays use PostgreSQL text literals such as `{a,b}`. Elements may contain commas, quotes, and `NULL`.
* `json`/`jsonb` and `hstore` are passed as text and parsed by PostgreSQL.
* `bit`/`varbit` values are strings of `0` and `1`.
* Prefer an explicit offset for `timestamptz`/`timetz` so the result does not depend on the JVM time zone.
* Identity columns are usually omitted: PostgreSQL generates the value.
* The `hstore` extension is required for `hstore`.

## dataStorageDiff

`<dataStorageDiff>` describes the changes after the test. The diff is row-based and contains only the
columns that changed.

### Insert

An inserted row shows the complete row:

```xml
<dataStorageDiff type="MapStringKeyObjectValue">
    <dataSource type="EntriesObjectKeyObjectValue">
        <entry change="added">
            <key>users_100</key>
            <value type="MapStringKeyObjectValue">
                <id type="Long">100</id>
                <name>Ivan</name>
                <email>ivan@example.com</email>
            </value>
        </entry>
    </dataSource>
</dataStorageDiff>
```

### Update

An update shows only the changed columns:

```xml
<dataStorageDiff type="MapStringKeyObjectValue">
    <dataSource type="EntriesObjectKeyObjectValue">
        <entry change="changed">
            <key>users_1</key>
            <value type="MapStringKeyObjectValue">
                <name>Ivan Updated</name>
            </value>
        </entry>
    </dataSource>
</dataStorageDiff>
```

### Delete

A deleted row must have a `nil` value:

```xml
<dataStorageDiff type="MapStringKeyObjectValue">
    <dataSource type="EntriesObjectKeyObjectValue">
        <entry change="deleted">
            <key>users_1</key>
            <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        </entry>
    </dataSource>
</dataStorageDiff>
```

### Row Keys

If the table has a primary key, the row key is:

```text
<table>_<primary key value>
```

Examples:

```text
users_1
orders_42
billing.invoices_7
```

For a composite primary key, the values are appended with `_`. Simple primary keys are easier to read.

The table part of the key follows `defaultSchema`: a table from `defaultSchema` is written without
the schema (`users_1`), while a table from another schema keeps the prefix (`billing.invoices_7`).
Unqualified names in configuration and fixtures resolve to `defaultSchema`; use `schema.table` to
address a table in another schema.

If there is no primary key, the row is identified by a hash of its entire content:

```text
nokey_data_20e4a4ea34a193578fdcbb989221baab40a137646521718fbe55d9df70db52ad
```

In a table without a primary key, an update appears as a deleted old row and an added new row. Add a
primary key if such diffs are inconvenient.

### Multiple Test Parts

Inside one case, `<dataStorageDiff>` can be specified for every `<test type="Part">`. Each part's diff
is relative to the state after the previous part:

```xml
<test type="Case" name="Accumulated changes between parts">
    <init type="SqlStorageInit" name="dataSource">
        <tablesToClear>users</tablesToClear>
    </init>

    <test type="Part" name="Insert">
        <!-- ... -->
        <dataStorageDiff>... added ...</dataStorageDiff>
    </test>

    <test type="Part" name="Update">
        <!-- ... -->
        <dataStorageDiff>... changed ...</dataStorageDiff>
    </test>

    <test type="Part" name="Delete">
        <!-- ... -->
        <dataStorageDiff>... deleted ...</dataStorageDiff>
    </test>
</test>
```

If the same row changes several times within one part, the diff shows the final state relative to the
start of the part. An insert followed by a deletion produces no diff entry.

### What Is Not Included

* Columns with `ignore: true`.
* Nested fields listed in `excludedFields`.
* The module's technical tables.
* Changes that bypass PostgreSQL triggers (for example, under
  `session_replication_role = 'replica'` or with disabled triggers).

Column values and serde settings are described in [Types and Serde](Types-and-Serde.md).

---
[← Back to documentation](../README.md)
