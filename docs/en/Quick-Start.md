Quick Start
===========

Step 1: Add the Dependency
--------------------------

```
xml  
<!-- pom.xml -->  
<dependency>  
    <groupId>io.github.dimkich</groupId>  
    <artifactId>integration-testing</artifactId>  
    <version>0.4.0</version>  
    <scope>test</scope>  
</dependency>  

```

Step 2: Create a Testable Service
---------------------------------

```
java  
package com.example.service;  

import org.springframework.stereotype.Service;  

@Service  
public class GreetingService {  
    
    public String greet(String name) {  
        return "Hello, " + name + "!";  
    }  
    
    public Integer add(Integer a, Integer b) {  
        return a + b;  
    }  
}  

```

Step 3: Create a Test File
--------------------------

Create a file at `src/test/resources/tests/greeting.xml`:

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<!-- @formatter:off -->  
<test type="Container">  
    <test type="Case" name="greeting">  
        <bean>greetingService</bean>  
        <method>greet</method>  
        <request>  
            <value>World</value>  
        </request>  
        <response>Hello, World!</response>  
    </test>  
    <test type="Case" name="addition">  
        <bean>greetingService</bean>  
        <method>add</method>  
        <request>  
            <value type="Integer">10</value>  
            <value type="Integer">20</value>  
        </request>  
        <response type="Integer">30</response>  
    </test>  
</test>  

```

> ⚠️ **Important:**
>
> * `type="String"` is **not specified** --- String is the default type
> * If `<response>` contains plain text without `type`, it is always treated as `String`
* `type` is only added for non-String types: `Integer`, `Boolean`, `BigDecimal`, etc.

Step 4: Create a Test Class
---------------------------

```
java  
package com.example;  

import io.github.dimkich.integration.testing.*;  
import org.junit.jupiter.api.DynamicNode;  
import org.junit.jupiter.api.TestFactory;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.test.context.SpringBootTest;  

import java.util.stream.Stream;  

@IntegrationTesting  
@SpringBootTest  
public class GreetingIntegrationTest {  
    
    private final DynamicTestBuilder dynamicTestBuilder;  
    
    @Autowired  
    public GreetingIntegrationTest(DynamicTestBuilder dynamicTestBuilder) {  
        this.dynamicTestBuilder = dynamicTestBuilder;  
    }  
    
    @TestFactory  
    Stream<DynamicNode> tests() throws Exception {  
        return dynamicTestBuilder.build("tests/greeting.xml");  
    }  
}  

```

💡 **Note:** The `@IntegrationTesting` annotation automatically imports `IntegrationTestConfig`, so you don't need to specify it in `@SpringBootTest(classes = ...)`.

Step 5: Run the Test
--------------------

```
bash  
mvn test -Dtest=GreetingIntegrationTest  

```

What Happens
------------

| Step |                                   Description                                   |
|------|---------------------------------------------------------------------------------|
| 1    | `@IntegrationTesting` activates the framework and loads `IntegrationTestConfig` |
| 2    | `DynamicTestBuilder` reads `greeting.xml`                                       |
| 3    | Parses XML into `Container` → `Case` hierarchy                                  |
| 4    | Finds Spring bean `greetingService`                                             |
| 5    | Calls method `greet("World")`                                                   |
| 6    | Compares result with `<response>`                                               |
| 7    | Reports success or failure in JUnit                                             |

Test Structure
--------------

```
test type="Container" (root, required)  
├── test type="Container" (nested container, optional)  
│   └── test type="Case" (test case)  
│       └── test type="Part" (test part, optional)  
└── test type="Case" (test case)  
    ├── bean: Spring bean name  
    ├── method: method name  
    ├── request: method arguments (list of <value>)  
    └── response: expected result (String by default)  

```

Example with Nesting
--------------------

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<!-- @formatter:off -->  
<test type="Container">  
    <test type="Container" name="group1">  
        <test type="Case" name="scenario1">  
            <bean>greetingService</bean>  
            <method>greet</method>  
            <request>  
                <value>Test</value>  
            </request>  
            <response>Hello, Test!</response>  
        </test>  
        <test type="Case" name="scenario2">  
            <test type="Part" name="step1">  
                <bean>greetingService</bean>  
                <method>add</method>  
                <request>  
                    <value type="Integer">5</value>  
                    <value type="Integer">5</value>  
                </request>  
                <response type="Integer">10</response>  
            </test>  
            <test type="Part" name="step2">  
                <bean>greetingService</bean>  
                <method>add</method>  
                <request>  
                    <value type="Integer">10</value>  
                    <value type="Integer">10</value>  
                </request>  
                <response type="Integer">20</response>  
            </test>  
        </test>  
    </test>  
    
    <!-- Disabled test -->  
    <test type="Case" name="disabled" disabled="true">  
        <bean>greetingService</bean>  
        <method>greet</method>  
        <request>  
            <value>Skip</value>  
        </request>  
    </test>  
</test>  

```

Data Types for Request and Response
-----------------------------------

|  Java Type   |                XML Format                 |      `type` Attribute      |
|--------------|-------------------------------------------|----------------------------|
| `String`     | `<value>text</value>`                     | **not required** (default) |
| `Integer`    | `<value type="Integer">1</value>`         | required                   |
| `Long`       | `<value type="Long">1</value>`            | required                   |
| `Boolean`    | `<value type="Boolean">true</value>`      | required                   |
| `BigDecimal` | `<value type="BigDecimal">1.23</value>`   | required                   |
| `null`       | `<value xmlns:xsi="..." xsi:nil="true"/>` | not required               |

💡 **Rule:** If `type` is not specified, the framework treats the value as `String`.

Handling Varargs in Requests
----------------

If the Spring bean method you are calling uses variable arguments (varargs), for example:  
public void rPush(String key, String... values)

In the XML test file, you **must** explicitly specify the type Object[] for the variable part of the arguments. Using ArrayList will cause a "Method not found" error because Java reflection expects an array for the varargs parameter.

**Correct Example:**
```
    <test type="Part" name="Setup">
        <bean>redisStringFacade</bean>
        <method>rPush</method>
        <request>list:key</request>
        <!-- Use Object[] to match varargs String... values -->
        <request type="Object[]">
            <item>v1</item>
            <item>v2</item>
            <item>v3</item>
        </request>
    </test>
```

Hooks (Optional)
----------------

Add to configuration for logging before/after test:

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    BeforeTest beforeTest() {  
        return test -> System.out.println("Before: " + test.getName());  
    }  
    
    @Bean  
    AfterTest afterTest() {  
        return test -> System.out.println("After: " + test.getName());  
    }  
}  

```

Common Problems
---------------

### Test Fails with Comparison Error

**Cause:** Result does not match expected value.  
**Solution:**

* Fix `<response>` in the XML test file
* Check data types in `<request>` (specify `type="..."` for non-String types)

### Test is Skipped Without Errors

**Cause:** Attribute `disabled="true"` on test or parent.  
**Solution:** Remove `disabled` or check inheritance of disabled state.

### `Method not found in bean`

**Cause:** Method not found by name and signature.  
**Solution:**

* Check method name in `<method>`
* Ensure types in `<request>` match method parameters
* For non-String types, always specify `type="..."`

### Error Running Tests

**Cause:** Missing `@SpringBootTest` annotation on test class.  
**Solution:** Add `@SpringBootTest` to the test class (`@IntegrationTesting` does not replace `@SpringBootTest`).