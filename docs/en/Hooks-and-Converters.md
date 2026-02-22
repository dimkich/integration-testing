Hooks and Converters (BeforeTest, AfterTest, TestConverter)
===========================================================

Overview
--------

The framework provides three functional interfaces for extending test behavior:

| Interface       | When Called                            | Purpose                                         | Executed for Container |
|-----------------|----------------------------------------|-------------------------------------------------|------------------------|
| `BeforeTest`    | Before each test execution             | Initialization, state setup, logging            | **Yes**                |
| `AfterTest`     | After each test execution              | Cleanup, resource deallocation, post-processing | **Yes**                |
| `TestConverter` | After test execution, before assertion | Transform test data for comparison              | **No**                 |

Important Features
------------------

### Hooks Are Not Inherited

`BeforeTest`, `AfterTest`, and `TestConverter` are **Spring beans** that:

* Execute **for every `Test`** (Container, Case, Part)
* **Do not participate in settings inheritance** (unlike `init`)
* Are called **always** , if the test is not disabled (`disabled`)
* Execution order is determined by the order of bean registration in Spring context

```
TestExecutor.before()  
    └── BeforeTest.before() ← called for each Container/Case/Part  

TestExecutor.runTest()  
    └── TestConverter.convert() ← called only for Case/Part  

TestExecutor.after()  
    └── AfterTest.after() ← called for each Container/Case/Part  

```

### TestConverter Executes Only for Non-Containers

Important difference for `TestConverter`:

| Test Type   | BeforeTest | AfterTest | TestConverter |
|-------------|------------|-----------|---------------|
| `Container` | **Yes**    | **Yes**   | **No**        |
| `TestCase`  | **Yes**    | **Yes**   | **Yes**       |
| `TestPart`  | **Yes**    | **Yes**   | **Yes**       |

**Reason:** `TestConverter` is designed to transform test data (`response`, `request`, etc.), but `Container` has no
such data --- it only groups child tests.

BeforeTest
----------

### Description

Functional interface for executing code **before** each test.

```
java  
@FunctionalInterface  
public interface BeforeTest {  
    void before(Test test) throws Exception;  
}  

```

### When Called

```
TestExecutor.before()  
    └── beforeConsumer()  
        ├── initializationService.beforeTest()  
        └── BeforeTest.before() ← called here (for Container, Case, Part)  

```

### Usage Example

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    BeforeTest logTestStart() {  
        return test -> System.out.println("Before: " + test.getName());  
    }  
    
    @Bean  
    BeforeTest clearDatabase() {  
        return test -> {  
            // Clear DB before each test (including containers)  
            jdbcTemplate.update("DELETE FROM users");  
            jdbcTemplate.update("DELETE FROM orders");  
        };  
    }  
    
    @Bean  
    BeforeTest setupTestData() {  
        return test -> {  
            // Setup test data  
            testDataStorage.insert("test_user", Map.of("id", 1, "name", "Test"));  
        };  
    }  
}  

```

### Execution Order

If multiple `BeforeTest` beans are registered, they execute **sequentially** in registration order:

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    @Order(1)  
    BeforeTest first() {  
        return test -> System.out.println("First: " + test.getName());  
    }  
    
    @Bean  
    @Order(2)  
    BeforeTest second() {  
        return test -> System.out.println("Second: " + test.getName());  
    }  
}  
// Output: First → Second  

```

AfterTest
---------

### Description

Functional interface for executing code **after** each test.

```
java  
@FunctionalInterface  
public interface AfterTest {  
    void after(Test test) throws Exception;  
}  

```

### When Called

```
TestExecutor.after()  
    └── afterConsumer()  
        ├── initializationService.afterTest()  
        └── AfterTest.after() ← called here (for Container, Case, Part)  

```

### Usage Example

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    AfterTest logTestEnd() {  
        return test -> System.out.println("After: " + test.getName());  
    }  
    
    @Bean  
    AfterTest logExecutionTime() {  
        return test -> {  
            long duration = test.getExecutionTime();  
            log.info("Test {} executed in {} ms", test.getName(), duration);  
        };  
    }  
    
    @Bean  
    AfterTest cleanupResources() {  
        return test -> {  
            // Cleanup temp files  
            Files.deleteIfExists(Path.of("/tmp/test-" + test.getName()));  
        };  
    }  
}  

```

### Execution Order

```
Test executed  
    ↓  
TestConverter applied  
    ↓  
Assertion.assertTestsEquals()  
    ↓  
AfterTest.after() ← here (for each Container/Case/Part)  

```

TestConverter
-------------

### Description

Functional interface for **transforming test data** before assertion.

```
java  
@FunctionalInterface  
public interface TestConverter {  
    void convert(Test test) throws Exception;  
}  

```

### When Called

```
TestExecutor.runTest()  
    ├── test.executeMethod() or sendInboundMessage()  
    ├── waitCompletion.waitCompletion()  
    ├── pollMessages()  
    ├── testDataStorages.getMapDiff()  
    ├── TestConverter.convert() ← called here (only for Case/Part)  
    └── assertion.assertTestsEquals()  

```

### Usage Example

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    TestConverter trimStrings() {  
        return test -> {  
            // Trim whitespace from string responses  
            if (test.getResponse() instanceof String) {  
                test.setResponse(((String) test.getResponse()).trim());  
            }  
        };  
    }  
    
    @Bean  
    TestConverter normalizeBigDecimal() {  
        return test -> {  
            // Normalize BigDecimal (remove trailing zeros)  
            if (test.getResponse() instanceof BigDecimal) {  
                test.setResponse(((BigDecimal) test.getResponse()).stripTrailingZeros());  
            }  
        };  
    }  
    
    @Bean  
    TestConverter sortCollections() {  
        return test -> {  
            // Sort collections for stable comparison  
            if (test.getResponse() instanceof List) {  
                List<?> list = (List<?>) test.getResponse();  
                if (!list.isEmpty() && list.get(0) instanceof Comparable) {  
                    list.sort(Comparator.naturalOrder());  
                }  
            }  
        };  
    }  
}  

```

### Using Test.custom

The `Test.custom` field is designed to store **additional readable data** about the test. This is especially useful
with `TestConverter`:

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    TestConverter addReadableCustomData() {  
        return test -> {  
            // Add readable representation of complex data  
            if (test.getResponse() instanceof ComplexObject) {  
                ComplexObject obj = (ComplexObject) test.getResponse();  
                test.addCustom("readableSummary", obj.toSummaryString());  
                test.addCustom("itemCount", obj.getItems().size());  
            }  
        };  
    }  
}  

```

**Why this is useful:**

| Problem                                               | Solution via `custom`               |
|-------------------------------------------------------|-------------------------------------|
| Complex objects in `response` are hard to read in XML | Add `custom` with brief description |
| Need to store metadata for debugging                  | Store in `custom`                   |
| Additional information needed for report              | Add to `custom`                     |

**XML example with custom:**

```
xml  
<test type="Case" name="complexTest">  
    <bean>service</bean>  
    <method>run</method>  
    <custom>  
        <readableSummary>Order created with 5 items</readableSummary>  
        <itemCount>5</itemCount>  
    </custom>  
    <response type="ComplexObject">  
        <!-- complex structure -->  
    </response>  
</test>  

```

### Converter Execution Order

If multiple `TestConverter` beans are registered, they execute **sequentially** :

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    TestConverter first() {  
        return test -> test.setResponse(((String) test.getResponse()).trim());  
    }  
    
    @Bean  
    TestConverter second() {  
        return test -> test.setResponse(((String) test.getResponse()).toLowerCase());  
    }  
}  
// trim() → toLowerCase()  

```

Complete Test Lifecycle Diagram
-------------------------------

```
┌─────────────────────────────────────────────────────────────┐  
│                    TestExecutor.before()                    │  
│  ├── test = expectedTest                                    │  
│  ├── assertion.setExpected(test)                            │  
│  ├── test.before()                                          │  
│  │   ├── initializationService.beforeTest()                 │  
│  │   └── BeforeTest.before() ← Hook 1 (for Container/Case/Part)│  
│  └── testDataStorages.setNewCurrentValue()                  │  
└─────────────────────────────────────────────────────────────┘  
                            ↓  
┌─────────────────────────────────────────────────────────────┐  
│              TestExecutor.runTest() (only Case/Part)        │  
│  ├── waitCompletion.start()                                 │  
│  ├── MockAnswer.enable()                                    │  
│  │   ├── sendInboundMessage() or executeMethod()            │  
│  │   └── waitCompletion.waitCompletion()                    │  
│  ├── pollMessages()                                         │  
│  ├── testDataStorages.getMapDiff()                          │  
│  ├── TestConverter.convert() ← Hook 2 (only for Case/Part)  │  
│  └── assertion.assertTestsEquals()                          │  
└─────────────────────────────────────────────────────────────┘  
                            ↓  
┌─────────────────────────────────────────────────────────────┐  
│                    TestExecutor.after()                     │  
│  ├── test.after()                                           │  
│  │   ├── initializationService.afterTest()                  │  
│  │   └── AfterTest.after() ← Hook 3 (for Container/Case/Part)│  
└─────────────────────────────────────────────────────────────┘  

```

Hooks vs Settings Comparison
----------------------------

| Characteristic              | BeforeTest/AfterTest/TestConverter | init (TestInit)                    |
|-----------------------------|------------------------------------|------------------------------------|
| **Type**                    | Spring beans                       | XML configuration                  |
| **Inheritance**             | **No** (executed for all)          | **Yes** (inherited top-down)       |
| **Execution for Container** | Yes                                | Yes                                |
| **Execution for Case**      | Yes                                | Yes                                |
| **Execution for Part**      | Yes                                | Yes                                |
| **State reset**             | No (depends on code)               | Yes (before each Case)             |
| **When to use**             | Logging, cleanup, transformation   | Data initialization, table cleanup |

Practical Example
-----------------

### Configuration with Hooks and Converters

```
java  
package com.example.config;  

import io.github.dimkich.integration.testing.*;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
import org.springframework.jdbc.core.JdbcTemplate;  

@Configuration  
public class TestHooksConfig {  
    
    private final JdbcTemplate jdbcTemplate;  
    
    public TestHooksConfig(JdbcTemplate jdbcTemplate) {  
        this.jdbcTemplate = jdbcTemplate;  
    }  
    
    @Bean  
    BeforeTest logAllTests() {  
        // Executed for Container, Case, Part  
        return test -> System.out.println("Before: " + test.getName() +  
                                          " (type=" + test.getType() + ")");  
    }  
    
    @Bean  
    BeforeTest clearTables() {  
        // Executed for Container, Case, Part  
        return test -> {  
            jdbcTemplate.update("DELETE FROM orders");  
            jdbcTemplate.update("DELETE FROM users");  
        };  
    }  
    
    @Bean  
    AfterTest logAllTestsEnd() {  
        // Executed for Container, Case, Part  
        return test -> System.out.println("After: " + test.getName());  
    }  
    
    @Bean  
    TestConverter normalizeResponse() {  
        // Executed ONLY for Case/Part  
        return test -> {  
            // Normalize response for stable comparison  
            if (test.getResponse() instanceof String) {  
                test.setResponse(((String) test.getResponse())  
                    .trim()  
                    .replaceAll("\\s+", " "));  
            }  
            
            // Add readable representation to custom  
            if (test.getResponse() instanceof Order) {  
                Order order = (Order) test.getResponse();  
                test.addCustom("orderId", order.getId());  
                test.addCustom("itemCount", order.getItems().size());  
            }  
        };  
    }  
}  

```

### XML Test

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<test type="Container">  
    <test type="Case" name="createOrder">  
        <bean>orderService</bean>  
        <method>create</method>  
        <request>  
            <value type="String">ORDER-123</value>  
        </request>  
        <custom>  
            <description>Order creation test</description>  
        </custom>  
        <response type="Order">  
            <id>ORDER-123</id>  
            <status>CREATED</status>  
        </response>  
    </test>  
</test>  

```

### Test Class

```
java  
package com.example;  

import com.example.config.TestHooksConfig;  
import io.github.dimkich.integration.testing.*;  
import org.junit.jupiter.api.DynamicNode;  
import org.junit.jupiter.api.TestFactory;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.test.context.SpringBootTest;  
import org.springframework.context.annotation.Import;  

import java.util.stream.Stream;  

@IntegrationTesting  
@SpringBootTest  
@Import(TestHooksConfig.class)  
public class OrderIntegrationTest {  
    
    private final DynamicTestBuilder dynamicTestBuilder;  
    
    @Autowired  
    public OrderIntegrationTest(DynamicTestBuilder dynamicTestBuilder) {  
        this.dynamicTestBuilder = dynamicTestBuilder;  
    }  
    
    @TestFactory  
    Stream<DynamicNode> tests() throws Exception {  
        return dynamicTestBuilder.build("tests/order.xml");  
    }  
}  

```

Common Problems
---------------

### BeforeTest Not Called

**Cause:** Bean not registered in Spring context.  
**Solution:**

* Ensure class with `@Bean` is annotated with `@Configuration`
* Import configuration into test class via `@Import`

### TestConverter Not Applied

**Cause:** Converter registered after test execution.  
**Solution:**

* Ensure `TestConverter` is registered as `@Bean` before tests run
* Check execution order via `@Order`

### Hooks Executed for Container

**Cause:** Expected that hooks are not executed for containers.  
**Solution:**

* This is **normal behavior** --- `BeforeTest` and `AfterTest` execute for **all** test types, including `Container`
* If hook is not called, check bean registration in Spring context

### TestConverter Called for Container

**Cause:** Expected that converter executes for all tests.  
**Solution:**

* `TestConverter` is **not called** for `Container` by design
* Containers have no data (`response`, `request`) to transform
* Use `BeforeTest`/`AfterTest` for container-level logic

### Settings Not Inherited in Hooks

**Cause:** Hooks do not participate in settings inheritance system.  
**Solution:**

* For inheritable settings, use `init` in XML
* Hooks execute the same way for all tests regardless of hierarchy

Recommendations
---------------

1. **BeforeTest** --- use for:
    * State cleanup (DB, files, cache) --- executes for Container, Case, Part
    * Test data setup
    * Test start logging (including containers)
2. **AfterTest** --- use for:
    * Resource cleanup --- executes for Container, Case, Part
    * Result logging (including containers)
    * Sending metrics/notifications
3. **TestConverter** --- use for:
    * Data normalization before comparison --- **only Case/Part**
    * Removing unstable fields (timestamp, id)
    * Masking sensitive data
    * **Adding readable data to `Test.custom`**
4. **Avoid** in hooks:
    * Complex business logic
    * Network calls
    * Long-running operations
5. **Remember:**
    * `BeforeTest` and `AfterTest` execute for **all** test types (Container, Case, Part)
    * `TestConverter` executes **only** for `TestCase` and `TestPart`

* Hooks are Spring beans, they are **not inherited** , but execute for every test  
