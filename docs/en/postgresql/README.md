PostgreSQL Integration Testing
==============================

[Russian version](../../ru/postgresql/README.md)

The module lets you test an application that works with PostgreSQL without writing assertions manually.
You prepare data before a test, invoke application code, and describe the expected database changes in
XML. The framework reads the changes, builds a diff, and restores the state between tests.

The module is intended for scenarios where the application and the test use the same database. A
separate application `DataSource` is not required: the framework opens its own JDBC connection using
the configured parameters.

## Features

* Prepare schemas and data with SQL files, inline SQL, and DBUnit datasets.
* Show row changes automatically in `dataStorageDiff`.
* Compare the state before and after a test instead of writing an assertion for every column.
* Support PostgreSQL types: arrays, `json`/`jsonb`, `hstore`, `bytea`, composite types, time,
  identity and generated columns.
* Configure serde per column and exclude technical fields from the diff.
* Restore the database to the required state before the next test without reloading everything when
  a cheaper revert is possible.

## How It Works

1. **Preparation.** `SqlStorageSetup` and `SqlStorageInit` run before the test: the schema is created,
   the DBUnit dataset is loaded, and SQL statements are applied.
2. **Execution.** The test calls an application method or sends a message. Changes the application
   makes in PostgreSQL are captured in a technical journal.
3. **Verification.** After the test, the framework reads the journal and compares the initial and
   final row states. The result is shown in `<dataStorageDiff>`.
4. **Transition to the next test.** The next `init` restores the database to its target state. If
   reverting rows is cheaper than reloading a table, the framework reverts them; otherwise it reloads
   the table from the DBUnit dataset.

## Setup

Add the module dependency:

```xml
<dependency>
    <groupId>io.github.dimkich.integration.testing.postgresql</groupId>
    <artifactId>integration-testing-postgresql</artifactId>
</dependency>
```

Enable PostgreSQL support with one annotation on the test class:

```java
package com.example.postgres;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.postgresql.EnableTestPostgreSQL;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

@EnableTestPostgreSQL
@SpringBootTest
@ActiveProfiles("test")
public class UserPostgresTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("tests/postgres-user.xml");
    }
}
```

`@EnableTestPostgreSQL` imports `postgresql.properties`, which enables embedded PostgreSQL from
Testcontainers by default. For an external database, set the connection values explicitly and disable
the embedded mode through test properties.

## Quick Start

### Connection Configuration

`dataSource` in the example is the framework connection name. It does not have to match an application
`DataSource` bean name, but it must match `name` in `<init>` and in `<dataStorageDiff>`.

```yaml
integration:
  testing:
    postgresql:
      connections:
        dataSource:
          host: "${embedded.postgresql.host}"
          port: "${embedded.postgresql.port}"
          database: "${embedded.postgresql.schema}"
          user: "${embedded.postgresql.user}"
          password: "${embedded.postgresql.password}"
          defaultSchema: public
    storage:
      storages:
        dataSource:
          keyType: [ STRING, OBJECT ]
          valueType: [ OBJECT, OBJECT ]
          changeType: [ false, true ]
```

`integration.testing.storage.storages` controls the XML diff shape. It can be omitted if not needed;
see [Configuration](Configuration.md) for details.

### Test File

```xml
<?xml version="1.0" encoding="UTF-8"?>
<test type="Container" name="Users">
    <!-- The schema and DBUnit dataset are registered once per container. -->
    <init type="SqlStorageSetup" name="dataSource">
        <sqlFilePath>schema.sql</sqlFilePath>
        <dbUnitPath>db-data.xml</dbUnitPath>
    </init>

    <test type="Case" name="Create user">
        <!-- Load the users table from db-data.xml. -->
        <init type="SqlStorageInit" name="dataSource">
            <tablesToLoad>users</tablesToLoad>
        </init>

        <bean>userService</bean>
        <method>create</method>
        <request type="Long">100</request>
        <request>Ivan</request>
        <request>ivan@example.com</request>

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
    </test>
</test>
```

The `dataStorageDiff` format and row key rules are described in
[Data and Diff](Data-and-Diff.md).

## Requirements

* JDK 17 and Spring Boot 3.x.
* PostgreSQL. The embedded mode requires a running Docker.
* The technical account must be able to create the service journal table, functions, and triggers,
  and to execute `ALTER SEQUENCE` and `SET session_replication_role`. See
  [Configuration](Configuration.md#environment-requirements).
* The `hstore` extension must be installed for `hstore` columns.
* `bytea` expects the standard `bytea_output = 'hex'`.

## Next

* [Configuration](Configuration.md) — connections, permissions, `columns`, `ignore`, and `excludedFields`.
* [Data and Diff](Data-and-Diff.md) — `SqlStorageSetup`, `SqlStorageInit`, DBUnit, and `dataStorageDiff`.
* [Types and Serde](Types-and-Serde.md) — PostgreSQL type representations and custom converters.
* [Troubleshooting](Troubleshooting.md) — empty diff, permission, conversion, and schema errors.

---
[← Back to documentation](../README.md)
