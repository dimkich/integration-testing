Test Initialization Subsystem Documentation
===========================================

Overview
--------

The test initialization subsystem provides a declarative mechanism for configuring the test environment state before
test execution. It allows you to define initialization actions in XML test files using the `<init>` element, which are
applied according to the test hierarchy (Container → Case → Part).

Supported Initialization Types
------------------------------

| Type             | Class            | Description                                                       |
|------------------|------------------|-------------------------------------------------------------------|
| `BeanInit`       | `BeanInit`       | Invokes methods on Spring beans for setup/teardown                |
| `DateTimeInit`   | `DateTimeInit`   | Sets or shifts the mocked system time (requires `@MockJavaTime`)  |
| `MockInit`       | `MockInit`       | Resets mock invocation counters                                   |
| `SqlStorageInit` | `SqlStorageInit` | Configures SQL storage: table access, data loading, SQL execution |

Common Attributes
-----------------

| Attribute | Type   | Description                                                                                  | Default      |
|-----------|--------|----------------------------------------------------------------------------------------------|--------------|
| `type`    | String | **Required** . Initialization type: `BeanInit`, `DateTimeInit`, `MockInit`, `SqlStorageInit` | ---          |
| `applyTo` | String | Scope of application: `TestCase`, `TestPart`, or `All`                                       | `All`        |
| `name`    | String | Storage name (for `SqlStorageInit` only)                                                     | `dataSource` |

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

DateTimeInit
------------

### Description

Manages mocked time for tests. Requires the `@MockJavaTime` annotation on the test class.

### Attributes

| Attribute     | Required | Description                                                     | Mutual Exclusivity                    |
|---------------|----------|-----------------------------------------------------------------|---------------------------------------|
| `dateTime`    | No       | Absolute time in ISO-8601 format (e.g., `2025-01-01T00:00:00Z`) | Mutually exclusive with `addDuration` |
| `addDuration` | No       | Time shift in ISO-8601 duration format (e.g., `PT1M`, `PT1H`)   | Mutually exclusive with `dateTime`    |

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

**Time progression in multi-step test:**

```
xml  
<test type="Case" name="timeProgression">  
    <init type="DateTimeInit" applyTo="TestPart" addDuration="PT1M"/>  
    <test type="Part" name="step1">  
        <response type="ZonedDateTime">2025-01-01T00:01:00Z</response>  
    </test>  
    <test type="Part" name="step2">  
        <response type="ZonedDateTime">2025-01-01T00:02:00Z</response>  
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

Configures SQL database state for integration tests: table access permissions, data loading from DBUnit XML, and custom
SQL execution.

### Attributes

| Attribute           | Required | Description                                               |
|---------------------|----------|-----------------------------------------------------------|
| `name`              | No       | Storage name (must match configured `SQLDataStorage`)     |
| `tablesToChange`    | No       | Comma-separated list of tables to set to `ALLOWED` access |
| `tablesToLoad`      | No       | Comma-separated list of tables to load data into          |
| `loadAllTables`     | No       | Load all available tables from DBUnit dataset             |
| `disableTableHooks` | No       | Execute SQL without triggering table hooks                |
| `sql`               | No       | List of SQL statements to execute                         |

### Child Elements

| Element        | Description                                                |
|----------------|------------------------------------------------------------|
| `<dbUnitPath>` | Path to DBUnit XML dataset file (classpath resource)       |
| `<tableHook>`  | Register a bean method to be called after table operations |
| `<sql>`        | SQL statement to execute (can be repeated)                 |

### Attributes for `<tableHook>`

| Attribute    | Required | Description                                 |
|--------------|----------|---------------------------------------------|
| `tableName`  | **Yes**  | Table name the hook applies to              |
| `beanName`   | **Yes**  | Spring bean name containing the hook method |
| `beanMethod` | **Yes**  | Method name to invoke                       |

### Table Access States

| State        | Description                                   |
|--------------|-----------------------------------------------|
| `ALLOWED`    | Table is accessible for read/write operations |
| `RESTRICTED` | Table access is blocked (default state)       |

### Execution Order

1. Apply `tablesToChange`: set tables to `ALLOWED`
2. Apply `tablesToLoad` / `loadAllTables`: load data from DBUnit
3. Execute custom `sql` statements
4. Invoke registered `tableHook` methods

### Example: Full Configuration

```
xml  
<init type="SqlStorageInit" name="dataSource">  
    <!-- Load dataset -->  
    <dbUnitPath>testData.xml</dbUnitPath>  
    
    <!-- Allow specific tables -->  
    <tablesToChange>users,orders</tablesToChange>  
    
    <!-- Load only specific tables from dataset -->  
    <tablesToLoad>users</tablesToLoad>  
    
    <!-- Register hook -->  
    <tableHook tableName="orders" beanName="orderService" beanMethod="afterLoad"/>  
    
    <!-- Execute custom SQL -->  
    <sql>UPDATE users SET status = 'active' WHERE id = 1</sql>  
</init>  

```

### Example: Disable Hooks for SQL

```
xml  
<init type="SqlStorageInit" name="dataSource" disableTableHooks="true">  
    <sql>DELETE FROM audit_log</sql>  
    <!-- SQL executes without triggering table hooks -->  
</init>  

```

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
3. **Absolute values override relative** : `dateTime` overrides `addDuration`

Best Practices
--------------

1. **Set time once in root Container** --- avoids redundant reinitialization
2. **Use `applyTo` for granular control** --- prevents unnecessary setup overhead
3. **Group related initializations** --- keep `BeanInit`, `SqlStorageInit` logically separated
4. **Avoid `disableTableHooks` unless necessary** --- hooks ensure data consistency
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

### SQL Hooks Not Triggered

**Cause:** `disableTableHooks="true"` or incorrect `tableName` in `<tableHook>`. **Solution:**
Remove `disableTableHooks` and verify table names match database schema exactly.

### Mock Counters Not Reset

**Cause:** `resetAll="false"` with mocks defined in parent scope. **Solution:** Set `resetAll="true"` or
define `MockInit` at the appropriate hierarchy level.  
