Test Initialization Subsystem Documentation
===========================================

Overview
--------

The test initialization subsystem provides a declarative mechanism for configuring the test environment state before
test execution. It allows you to define initialization actions in XML test files using the `<init>` element, which are
applied according to the test hierarchy (Container → Case → Part).

Supported Initialization Types
------------------------------

| Type                | Class               | Description                                                         |
|---------------------|---------------------|---------------------------------------------------------------------|
| `BeanInit`          | `BeanInit`          | Invokes methods on Spring beans for setup/teardown                  |
| `BeanPropertyInit`  | `BeanPropertyInit`  | Sets bean fields/properties for a test and restores them afterward  |
| `DateTimeInit`      | `DateTimeInit`      | Sets or shifts the mocked system time (requires `@MockJavaTime`)    |
| `MockInit`          | `MockInit`          | Resets mock invocation counters                                     |
| `SqlStorageSetup`   | `SqlStorageSetup`   | Configures SQL storage: SQL scripts and DBUnit datasets             |
| `SqlStorageInit`    | `SqlStorageInit`    | Configures SQL storage: table clearing, data loading, SQL execution |
| `OnTableChangeInit` | `OnTableChangeInit` | Refreshes the application cache of stale tables                     |

Common Attributes
-----------------

| Attribute | Type   | Description                                                                                                                          | Default      |
|-----------|--------|--------------------------------------------------------------------------------------------------------------------------------------|--------------|
| `type`    | String | **Required** . Initialization type: `BeanInit`, `DateTimeInit`, `MockInit`, `SqlStorageSetup`, `SqlStorageInit`, `OnTableChangeInit` | ---          |
| `applyTo` | String | Scope of application: `TestCase`, `TestPart`, or `All`                                                                               | `All`        |
| `name`    | String | Storage name (for `SqlStorageInit` only)                                                                                             | `dataSource` |

BeanInit
--------

### Description

Invokes specified methods on Spring beans before test execution.

### XML Structure

```
xml  
<init type="BeanInit">  
    <bean name="beanName" method="methodName"/>  
    <bean name="anotherBean" method="setup"/>  
</init>  

```

### Attributes for `<bean>`

| Attribute | Required | Description           |
|-----------|----------|-----------------------|
| `name`    | **Yes**  | Spring bean name      |
| `method`  | **Yes**  | Method name to invoke |

### Example

```
xml  
<test type="Container">  
    <init type="BeanInit">  
        <bean name="databaseCleaner" method="clearAll"/>  
        <bean name="testDataLoader" method="loadFixtures"/>  
    </init>  
    <test type="Case" name="testCase">  
        <!-- Methods are invoked before this test -->  
        <bean>service</bean>  
        <method>run</method>  
        <response>ok</response>  
    </test>  
</test>  

```

BeanPropertyInit
----------------

### Description

Sets fields and JavaBean properties of Spring beans for the time of a test and restores the original values
afterward. Unlike `BeanInit`, this initialization is a **state**: the original value is captured before the first
modification and written back when the declaration leaves the scope (test case or container).

### XML Structure

```
xml
<init type="BeanPropertyInit" bean="candleJobFactory">
    <property name="startUpHistoryAmount" type="PeriodDuration">PT0M</property>
    <property name="dynamicCandlesFlag" type="Boolean">true</property>
</init>
```

Several beans may be configured by one declaration:

```
xml
<init type="BeanPropertyInit">
    <property bean="candleIteratorFactory" name="dynamicCandlesFlag" type="Boolean">true</property>
    <property bean="candleJobFactory" name="startUpHistoryAmount" type="PeriodDuration">PT0M</property>
</init>
```

### Attributes for `<init>`

| Attribute | Required | Description                                                      |
|-----------|----------|------------------------------------------------------------------|
| `bean`    | No       | Default Spring bean name for all nested `<property>` elements    |
| `name`    | No       | Alias for `bean` (does not override an explicitly set `bean`)    |
| `applyTo` | No       | Scope: `TestCase` or `TestPart`                                  |

### Attributes for `<property>`

| Attribute | Required | Description                                                    |
|-----------|----------|----------------------------------------------------------------|
| `bean`    | No       | Bean name; overrides the `bean` of `<init>`                    |
| `name`    | **Yes**  | Field name or JavaBean property name                           |
| `type`    | No       | Type of the value (as for any other test element)              |

### Value Forms

```
xml
<!-- null: an empty element (xsi:nil would nullify the <property> itself) -->
<property name="optionalRule"/>

<!-- collection: items are nested into the element body -->
<property name="allowedCurrencies" type="ArrayList">
    <value>RUB</value>
    <value>USD</value>
</property>
```

### JSON

```json
{
  "type": "BeanPropertyInit",
  "bean": "candleJobFactory",
  "property": [
    {"name": "startUpHistoryAmount", "value": ["PeriodDuration", "PT0M"]},
    {"name": "dynamicCandlesFlag", "value": true}
  ]
}
```

### Rules

* Values are written through a setter when it is compatible, otherwise through a private field; reading uses a
  getter / `is`-getter, then a private field. AOP proxies are unwrapped to the target object.
* `null` is written as is. A primitive field or setter cannot hold it, so such a declaration fails with an error.
* The baseline is captured once, before the first modification, and kept for the whole life of the Spring context.
* A nested declaration overrides the inherited value until it leaves its scope: after a case the value returns to the
  value of the container, after the container to the original Spring value.
* As with `DateTimeInit`, `TestPart`-level changes **accumulate** inside a case: they are reverted when the case ends,
  not between parts.
* The property must be readable (getter or field), otherwise the original value cannot be captured and an error is
  raised. Only singleton beans are supported.
* In XML the `type` attribute names the type of the unwrapped value: unwrapped values use `utype` by default, here it is
  overridden with `@UnwrappedTypeProperty("type")`.

DateTimeInit
------------

### Description

Manages mocked time for tests. Requires the `@MockJavaTime` annotation on the test class.

### Attributes

| Attribute     | Required | Description                                                     |
|---------------|----------|-----------------------------------------------------------------|
| `dateTime`    | No       | Absolute time in ISO-8601 format (e.g., `2025-01-01T00:00:00Z`) |
| `addDuration` | No       | Time shift in ISO-8601 duration format (e.g., `PT1M`, `PT1H`)   |

Both attributes may be specified in the same `<init>`: the resulting time is `dateTime + addDuration`.
An absolute `dateTime` is always a hard reset: it discards any `addDuration` accumulated before it in the same or an
enclosing scope. Therefore, declare `dateTime` before `addDuration` when both are separate `<init>` elements.

`addDuration` requires a base time. If no `dateTime` is defined in the current or an enclosing scope, applying the
initialization fails with an error. In practice this means every test file that uses `DateTimeInit` should define an
absolute `dateTime` in the root container.

### Duration Format (ISO-8601)

* `PT1M` --- 1 minute
* `PT1H` --- 1 hour
* `P1D` --- 1 day
* `PT30S` --- 30 seconds

### Examples

**Fixed time for all tests:**

```
xml  
<test type="Container">  
    <init type="DateTimeInit" dateTime="2025-01-01T00:00:00Z"/>  
    <test type="Case" name="fixedTimeTest">  
        <bean>dateTimeService</bean>  
        <method>getDateTime</method>  
        <response type="ZonedDateTime">2025-01-01T00:00:00Z</response>  
    </test>  
</test>  

```

**Time progression in multistep test:**

```
xml  
<test type="Container">  
    <!-- Base time is required for addDuration -->  
    <init type="DateTimeInit" dateTime="2025-01-01T00:00:00Z"/>  
    <test type="Case" name="timeProgression">  
        <init type="DateTimeInit" applyTo="TestPart" addDuration="PT1M"/>  
        <test type="Part" name="step1">  
            <response type="ZonedDateTime">2025-01-01T00:01:00Z</response>  
        </test>  
        <test type="Part" name="step2">  
            <response type="ZonedDateTime">2025-01-01T00:02:00Z</response>  
        </test>  
    </test>  
</test>  

```

MockInit
--------

### Description

Resets mock invocation counters tracked by the `MockInvoke` system.

### Attributes

| Attribute  | Type    | Description                                                     | Default |
|------------|---------|-----------------------------------------------------------------|---------|
| `resetAll` | Boolean | Reset all mock counters vs. only those defined in current scope | `false` |

### Example

```
xml  
<test type="Container">  
    <mockInvoke name="idGenerator" method="generate">  
        <result><return type="String">ID-001</return></result>  
    </mockInvoke>  
    
    <test type="Case" name="resetMocks">  
        <init type="MockInit" resetAll="true"/>  
        <!-- All mock counters are reset before this test -->  
        <bean>service</bean>  
        <method>run</method>  
    </test>  
</test>  

```

SqlStorageInit
--------------

### Description

Configures SQL database state for integration tests: clearing/loading tables from a DBUnit XML
dataset, and custom SQL execution.

### Attributes

| Attribute        | Required | Description                                            |
|------------------|----------|--------------------------------------------------------|
| `name`           | No       | Storage name (must match configured `SQLDataStorage`)  |
| `tablesToClear`  | No       | Comma-separated list of tables to clear (set to empty) |
| `tablesToLoad`   | No       | Comma-separated list of tables to load data into       |
| `loadAllTables`  | No       | Load all available tables from DBUnit dataset          |
| `sql`            | No       | List of SQL statements to execute                      |

### Child Elements

| Element        | Description                                          |
|----------------|------------------------------------------------------|
| `<dbUnitPath>` | Path to DBUnit XML dataset file (classpath resource) |
| `<sql>`        | SQL statement to execute (can be repeated)           |

### Table Data States

| State     | Description                                          |
|-----------|------------------------------------------------------|
| `CLEARED` | Table data is deleted (default for `tablesToClear`)  |
| `LOADED`  | Table data is loaded from the DBUnit dataset         |

### Execution Order

1. Apply `tablesToClear`: clear the listed tables
2. Apply `tablesToLoad` / `loadAllTables`: load data from DBUnit
3. Execute custom `sql` statements
4. Advance the data revision of the modified tables; an `OnTableChangeInit` refreshes their caches later

### State History and Rollback

Every initialization contributes an ordered `MutationLog` to the storage state: `LOAD`/`CLEAR`
baseline declarations, SQL mutations and the mutations the system under test produces during a test.
The table statuses live in a separate `TableBaseline`. Every `StorageMutation` carries the tables it
touched and its revert commands per table. The mutation attribution is built once, when the change
journal is read after the test (the only read of a test, see below): records without a tag are the
mutations of the system under test, tagged records belong to the applied SQL mutations. Tables
touched by a `<sql>` block are **not parsed** from the statement text: the journal content defines
them.

Before a new test `StorageState.planTransition` computes a `TransitionPlan` from the source and
target states. `TransitionAnalyzer` and `TableTransition` find the structural common prefix, compare
the baselines, evaluate whether each table can be rolled back cheaply enough and cascade the
necessary baseline resets. A mutation shared by several tables is kept only when it is common to all
of them; otherwise every table it touches is rolled back to before the mutation and the mutation is
applied exactly once. A rollback **never crosses a baseline change**: a table that had a
`LOAD`/`CLEAR` applied after the common prefix is not rolled back row by row, but reloaded or cleared
instead. The plan contains only commands that really change data: an empty revert command does not
invalidate a table.

Applying a transition has four ordered phases:

1. untagged rollback of the old tail and clearing of the reset tables;
2. DBUnit load of the target fixtures;
3. untagged sequence restart;
4. tagged target SQL mutations.

The tag is put into the session variable `it.node` and lands in the `node` column of the
`_it_postgres_journal_<random>` journal, so the next read can attribute the records to the mutations.

For the trigger based delta storage the rollback uses the prebuilt commands of the mutations: as long
as reverting is cheaper than reloading the fixture (the summed number of audited rows of the tail
mutations is below the fixture size plus one) the commands are executed; otherwise — when the change
cannot be reverted or when the tail crossed a `LOAD`/`CLEAR` boundary — the table is reloaded from the
DBUnit dataset or cleared according to its baseline. The revert commands are set
based (the records are squashed per primary key), so their number is proportional to the kinds of
changes, not to the number of records. Generated columns do not force a reload:
the framework excludes `GENERATED ALWAYS AS (...) STORED` columns from the inverse `INSERT`/`UPDATE`
(PostgreSQL recomputes them from the base columns) and restores `GENERATED ALWAYS AS IDENTITY`
values with `OVERRIDING SYSTEM VALUE` (it deletes and re-inserts the row when the identity value
actually changed). Storages without an audit log fall back to the plain table-diff executor.

#### Table cache synchronization

The application cache is modelled through monotonic table revisions. For every table the framework
keeps two revisions: the database revision advanced by a change made by the test framework
(`LOAD`/`CLEAR`, a `<sql>` mutation, a non-empty rollback or `clearTables`) and the revision
acknowledged by the application cache. While the database revision is greater, the cache is
considered stale. An `OnTableChangeInit` performs the synchronisation (see below): for every stale
table in its scope it invokes the configured action — **a bean method call only, no database I/O** —
and acknowledges the revision.

Mutations produced by the system under test are recorded in the mutation log (together with their
revert commands) but do not advance the database revision: the SUT tracks its own changes. This also
means that such a mutation does not acknowledge an earlier framework change, so a stale cache stays
stale until an action really synchronises it. If a later test rolls the SUT changes back, that
rollback is a framework change and the action is invoked.

### Example: Full Configuration

```
xml
<init type="SqlStorageInit" name="dataSource">
    <!-- Load dataset -->
    <dbUnitPath>testData.xml</dbUnitPath>

    <!-- Clear specific tables -->
    <tablesToClear>users,orders</tablesToClear>

    <!-- Load only specific tables from dataset -->
    <tablesToLoad>users</tablesToLoad>

    <!-- Execute custom SQL -->
    <sql>UPDATE users SET status = 'active' WHERE id = 1</sql>
</init>

```

OnTableChangeInit
--------------

### Description

Refreshes the application cache of a SQL storage: invokes the configured bean methods for the tables
whose database revision is newer than the acknowledged cache revision. This is an **action**, not a
state: it runs every time it is in scope and does nothing when no table is stale.

### Attributes

| Attribute | Required | Description                                                             |
|-----------|----------|-------------------------------------------------------------------------|
| `name`    | **Yes**  | Storage name (must match configured `SQLDataStorage`)                   |
| `applyTo` | No       | Level the action expands to (`TestContainer` / `TestCase` / `TestPart`) |

### Child Elements

| Element           | Description                                                                       |
|-------------------|-----------------------------------------------------------------------------------|
| `<onTableChange>` | `tableName` + `beanName` + `beanMethod`; an empty list disables inherited actions |

### Example

```
xml
<!-- Declare once on a container: refresh the cache for every test case -->
<init type="OnTableChangeInit" name="dataSource" applyTo="TestCase">
    <onTableChange tableName="users" beanName="cacheService" beanMethod="reloadUsers"/>
    <onTableChange tableName="orders" beanName="cacheService" beanMethod="reloadOrders"/>
</init>

<!-- Cancel the inherited actions for a single test -->
<init type="OnTableChangeInit" name="dataSource"/>

```

### Scope and cancellation

The action is expanded through `applyTo` and inherited down the tree. A more specific declaration
replaces the inherited one **for its own storage** (`name`): the `onTableChange` list is replaced as a
whole, so an `OnTableChangeInit` with an empty list disables the actions for its subtree. Different
storages do not clash — each is overridden by its own `name`.

Actions are invoked only for tables whose cache is stale (see "Table cache synchronization"); when
nothing changed no action is invoked. The initialization runs after `SqlStorageInit` and `BeanInit`,
so it sees the data of every storage already applied.

> **Limitation.** Two operations on the same table in one test — first with an action, then without —
> cannot be expressed: the states of same-type inits are merged. For that rare case use two
> `<test type="Part">`.

Inheritance and Scope (`applyTo`)
---------------------------------

Initialization settings are inherited **top-down** through the test hierarchy. The `applyTo` attribute controls which
test levels receive the initialization:

| `applyTo` Value | Applied to Container | Applied to Case | Applied to Part |
|-----------------|----------------------|-----------------|-----------------|
| `All` (default) | ✅                    | ✅               | ✅               |
| `TestCase`      | ❌                    | ✅               | ❌               |
| `TestPart`      | ❌                    | ❌               | ✅               |

### State Reset Behavior

| Test Level | State Reset Before Execution               |
|------------|--------------------------------------------|
| `TestCase` | **Yes** --- isolated context               |
| `TestPart` | **No** --- shares state with sibling Parts |

### Example: Scope Demonstration

```
xml  
<test type="Container">  
    <!-- Applied to all levels -->  
    <init type="BeanInit" applyTo="All">  
        <bean name="globalSetup" method="init"/>  
    </init>  
    
    <!-- Applied only to TestCase level -->  
    <init type="BeanInit" applyTo="TestCase">  
        <bean name="caseSetup" method="prepare"/>  
    </init>  
    
    <test type="Case" name="multiStep">  
        <!-- Applied only to TestPart level -->  
        <init type="BeanInit" applyTo="TestPart">  
            <bean name="partSetup" method="stepInit"/>  
        </init>  
        
        <test type="Part" name="step1">  
            <!-- Receives: globalSetup, caseSetup, partSetup -->  
        </test>  
        <test type="Part" name="step2">  
            <!-- Receives: globalSetup, caseSetup, partSetup (state NOT reset) -->  
        </test>  
    </test>  
    
    <test type="Case" name="independent">  
        <!-- Receives: globalSetup, caseSetup (state IS reset) -->  
    </test>  
</test>  

```

Priority Rules
--------------

When multiple `<init>` elements target the same test:

1. **More specific scope wins** : `TestPart` > `TestCase` > `All`
2. **Later declaration wins** within the same scope
3. **Absolute values are hard resets** : `dateTime` overrides `addDuration` and discards the accumulated shift

Best Practices
--------------

1. **Set time once in root Container** --- avoids redundant reinitialization
2. **Use `applyTo` for granular control** --- prevents unnecessary setup overhead
3. **Group related initializations** --- keep `BeanInit`, `SqlStorageInit` logically separated
4. **Keep an `OnTableChangeInit` in scope unless you need a stale cache** --- actions ensure data consistency
5. **Use `tablesToLoad` instead of `loadAllTables`** --- faster test execution
6. **Document custom SQL** --- add comments explaining non-obvious `sql` statements

Troubleshooting
---------------

### Initialization Not Applied

**Cause:** `applyTo` scope mismatch or incorrect hierarchy placement. **Solution:** Verify the `<init>` element is
placed at the correct level and `applyTo` matches the target test type.

### Time Not Mocked

**Cause:** `@MockJavaTime` annotation missing on test class. **Solution:** Add `@MockJavaTime` to the test class and
ensure `DateTimeInit` uses valid ISO-8601 format.

### Table Change Actions Not Triggered

**Cause:** an incorrect `tableName` in `<onTableChange>`, no `OnTableChangeInit` in scope, or an overlapping
`OnTableChangeInit` with an empty list. **Solution:** verify table names match the database schema exactly
and that an `OnTableChangeInit` is visible in the current test. When actions are intentionally disabled with
an empty `OnTableChangeInit` they are deferred: the affected tables stay stale and their actions fire on the
next test where the initialization is active again.

### Mock Counters Not Reset

**Cause:** `resetAll="false"` with mocks defined in parent scope. **Solution:** Set `resetAll="true"` or
define `MockInit` at the appropriate hierarchy level.  
