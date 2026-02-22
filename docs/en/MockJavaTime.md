@MockJavaTime Documentation
===========================

Overview
--------

**@MockJavaTime** is an annotation for integration testing that allows deterministic mocking of the Java Time API. When
this annotation is used, calls to Java Time API classes (such as `Clock`, `Instant`, `LocalDateTime`) return
predictable, test-controlled values instead of relying on the real system time.  
Time is configured via XML test configuration using the `<init type="DateTimeInit">` element.

Usage
-----

### Connection

To activate time mocking, add the `@MockJavaTime` annotation to the test class:

```
java  
@MockJavaTime  
@SpringBootTest  
public class MyTimeSensitiveTest {  
    // tests with mocked time  
}  

```

### Time Setup

Time is set in the test XML file inside the `<init>` element with the attribute `type="DateTimeInit"`. There are two
ways to manage time:

1. **Absolute time** (`dateTime`) --- setting a specific date and time.
2. **Time shift** (`addDuration`) --- adding a duration to the current time.

Annotation Attributes
---------------------

| Attribute | Type       | Description                                                                                   | Default Value      |
|-----------|------------|-----------------------------------------------------------------------------------------------|--------------------|
| `value()` | `String[]` | Prefixes of class/package names for which `System.currentTimeMillis()` calls should be mocked | `{}` (empty array) |

### Details of the `value()` Attribute

Each value is interpreted as a "starts with" pattern for the fully qualified class name:

* `"com.example"` --- mocks all classes in the `com.example` package tree
* `"com.example.service.TimeService"` --- mocks that class and its inner classes

**Usage Example:**

```
java  
@MockJavaTime({  
    "com.example.myapp",          // whole package hierarchy  
    "org.thirdparty.lib.Client"   // concrete class and its inner classes  
})  
public class MyTimeSensitiveTest {  
    // tests with mocked time  
}  

```

**When to use `value()`:**

| Scenario                                          | Configuration                            |
|---------------------------------------------------|------------------------------------------|
| Mocking only Java Time API                        | `@MockJavaTime` (without parameters)     |
| Mocking `System.currentTimeMillis()` in your code | `@MockJavaTime("com.myapp")`             |
| Mocking in a third-party library                  | `@MockJavaTime("org.thirdparty.lib")`    |
| Mocking a specific class                          | `@MockJavaTime("com.myapp.TimeService")` |

⚠️ **Important:** The `value()` attribute is only needed if you want to mock `System.currentTimeMillis()` calls in user
code or third-party libraries. For standard Java Time API mocking (`Clock`, `Instant`, `LocalDateTime`, etc.), it is
sufficient to use `@MockJavaTime` without parameters.

Standard Usage Scenario
-----------------------

**Recommended approach:** Set a fixed time once in the root container and do not change it in child tests.

```
xml  
<?xml version='1.1' encoding='UTF-8'?>  
<test type="Container">  
    <!-- Set time once for all tests -->  
    <init type="DateTimeInit" dateTime="2025-01-01T00:00:00Z"/>  
    
    <test type="Case" name="test1">  
        <bean>myService</bean>  
        <method>doSomething</method>  
        <response>ok</response>  
    </test>  
    
    <test type="Case" name="test2">  
        <bean>myService</bean>  
        <method>doSomethingElse</method>  
        <response>ok</response>  
    </test>  
</test>  

```

**Advantages of this approach:**

| Advantage          | Description                                              |
|--------------------|----------------------------------------------------------|
| **Predictability** | All tests work with the same time                        |
| **Isolation**      | Each `TestCase` gets state reset, but time remains fixed |
| **Simplicity**     | No need to duplicate `<init>` in every test              |
| **Performance**    | Minimal initialization overhead                          |

**When to deviate from the standard scenario:**

| Situation                               | Solution                                 |
|-----------------------------------------|------------------------------------------|
| Tests require different times           | Use `dateTime` in specific `TestCase`    |
| Need to check state evolution over time | Use `addDuration` in `TestPart`          |
| Test depends on real time               | Do not use `@MockJavaTime` for this test |

Usage Examples
--------------

### Example 1: Test with Fixed Time (Standard Scenario)

```
xml  
<?xml version='1.1' encoding='UTF-8'?>  
<test type="Container">  
    <!-- Time is set once for all tests -->  
    <init type="DateTimeInit" dateTime="2025-01-01T00:00:00Z"/>  
    
    <test type="Case" name="fixed time">  
        <bean>dateTimeService</bean>  
        <method>getDateTime</method>  
        <response type="ZonedDateTime">2025-01-01T00:00:00Z</response>  
    </test>  
</test>  

```

### Example 2: Time Progression in Tests

Using `addDuration` to shift time relative to the previous state or base time.

```
xml  
<?xml version='1.1' encoding='UTF-8'?>  
<test type="Container">  
    <!-- Base time initialization for the container -->  
    <init type="DateTimeInit" dateTime="2025-01-01T00:00:00Z"/>  
    
    <test type="Case" name="simple increment">  
        <!-- Shift by 1 minute for all Parts inside this case -->  
        <init type="DateTimeInit" applyTo="TestPart" addDuration="PT1M"/>  
        
        <test type="Part" name="step 1">  
            <bean>dateTimeService</bean>  
            <method>getDateTime</method>  
            <response type="ZonedDateTime">2025-01-01T00:01:00Z</response>  
        </test>  
        <test type="Part" name="step 2">  
            <bean>dateTimeService</bean>  
            <method>getDateTime</method>  
            <response type="ZonedDateTime">2025-01-01T00:02:00Z</response>  
        </test>  
    </test>  
</test>  

```

### Example 3: Combined Usage

Combining absolute time and shift at different hierarchy levels.

```
xml  
<?xml version='1.1' encoding='UTF-8'?>  
<test type="Container">  
    <test type="Case" name="increment with additional init">  
        <init type="DateTimeInit" applyTo="TestPart" addDuration="PT1M"/>  
        
        <test type="Part" name="step 1">  
            <bean>dateTimeService</bean>  
            <method>getDateTime</method>  
            <response type="ZonedDateTime">2025-01-01T00:01:00Z</response>  
        </test>  
        
        <test type="Part" name="step 2">  
            <!-- Override absolute time -->  
            <init type="DateTimeInit" dateTime="2025-02-02T00:00:00Z"/>  
            <bean>dateTimeService</bean>  
            <method>getDateTime</method>  
            <response type="ZonedDateTime">2025-02-02T00:00:00Z</response>  
        </test>  
        
        <test type="Part" name="step 3">  
            <!-- Shift applies to the new base time -->  
            <bean>dateTimeService</bean>  
            <method>getDateTime</method>  
            <response type="ZonedDateTime">2025-02-02T00:01:00Z</response>  
        </test>  
    </test>  
</test>  

```

Supported Java Time Classes
---------------------------

The following classes are mocked when using `@MockJavaTime`:

| Class                     | Description               |
|---------------------------|---------------------------|
| `java.time.Clock`         | All Clock implementations |
| `java.time.Instant`       | Instants of time          |
| `java.time.LocalDateTime` | Local date-time           |
| `java.time.ZonedDateTime` | Date-time with time zone  |
| `java.util.Date`          | Legacy date class         |
| `java.util.Calendar`      | Calendar                  |
| `java.util.TimeZone`      | Time zone                 |

Lifecycle
---------

* **Initialization:** Mocking is activated before tests run in the class annotated with `@MockJavaTime`.
* **Reset:** After all tests in the class complete, time is reset to system time.
* **Isolation:** Time settings specified in `<init>` are applied according to the test hierarchy (Container → Case →
  Part). Time state resets between `TestCase`, but is preserved between `TestPart` within one `TestCase` (unless
  specified otherwise).

Duration Format
---------------

The `addDuration` attribute uses the standard ISO-8601 format:

* `PT1M` --- 1 minute
* `PT1H` --- 1 hour
* `P1D` --- 1 day
* `PT30S` --- 30 seconds

Troubleshooting
---------------

### Time is not mocked in a third-party library

**Cause:** `System.currentTimeMillis()` calls in third-party code are not intercepted by default.  
**Solution:** Specify the library package in the `value()` attribute:

```
java  
@MockJavaTime({"org.thirdparty.lib"})  
public class MyTest {  
    // ...  
}  

```

### Conflict between tests

**Cause:** Time is not resetting between tests.  
**Solution:** Ensure `@MockJavaTime` is at the class level, not the method level. Reset happens automatically after all
tests in the class.

### `System.currentTimeMillis()` is not mocked

**Cause:** By default, only Java Time API classes are mocked.  
**Solution:** To mock `System.currentTimeMillis()`, specify packages in `value()`:

```
java  
@MockJavaTime({"com.myapp.service"})  
public class MyTest {  
    // ...  
}  

```

Recommendations
---------------

1. **Use `@MockJavaTime` without parameters** for standard Java Time API mocking.
2. **Set time in the root Container** --- this is the standard scenario that covers most use cases.
3. **Use `value()` only when necessary** for mocking `System.currentTimeMillis()` in your code or third-party libraries.
4. **Use `addDuration` for time progression** inside multi-step tests (`TestPart`).
5. **Check package name uniqueness** in `value()` --- use fully qualified names.
6. **Do not use `<mockInvoke>` for time** --- use `@MockJavaTime` and `DateTimeInit` for time management.  
