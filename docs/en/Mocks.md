Mocks in Integration Tests (TestBeanMock, TestConstructorMock, TestStaticMock)
==============================================================================

Overview
--------

The framework provides three annotations for configuring mocks based on Mockito:

| Annotation             | Purpose                | Scope                                                |
|------------------------|------------------------|------------------------------------------------------|
| `@TestBeanMock`        | Mocking Spring beans   | Bean instances in the context                        |
| `@TestConstructorMock` | Mocking constructors   | Any objects created through constructors (not beans) |
| `@TestStaticMock`      | Mocking static methods | Static methods of classes                            |

All annotations:

* Are applied at the test class level
* Support repetition (repeatable)
* Integrate with the `MockInvoke` system for configuring expected calls in XML

TestBeanMock
------------

### Description

Mocks or creates a spy for a **Spring bean** by name or type.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(TestBeanMock.List.class)  
public @interface TestBeanMock {  
    String name() default "";                    // Bean name in Spring context  
    Class<?> mockClass() default Null.class;     // Bean type (if name not specified)  
    String mockClassName() default "";           // FQN of type (alternative to mockClass)  
    String[] methods() default {};               // Specific methods to mock  
    boolean spy() default false;                 // Use spy instead of mock  
    boolean cloneArgsAndResult() default false;  // Deep clone arguments/results  
}  

```

### When to Use

**Use `@TestBeanMock` when you need to mock a Spring bean:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestBeanMock(name = "paymentService")  
public class OrderTest {  
    // paymentService will be replaced with a mock  
}  

```

### Usage Examples

**Mocking a bean by name:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestBeanMock(name = "paymentService")  
public class OrderTest {  
    // paymentService will be replaced with a mock  
}  

```

**Multiple mocks:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestBeanMock(name = "emailService")  
@TestBeanMock(name = "smsService", spy = true)  
public class NotificationTest {  
    // emailService --- mock, smsService --- spy  
}  

```

**Spy for a bean by type:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestBeanMock(mockClass = UserService.class, spy = true)  
public class UserTest {  
    // All beans of type UserService will be spies  
}  

```

### When to Use Spy

`spy = true` is used when you need to **sometimes execute the real method and sometimes not** :

```
java  
@TestBeanMock(name = "paymentService", spy = true)  

```

**Typical scenarios:**

* Throwing exceptions under certain conditions
* Partially preserving real logic
* Verifying calls while maintaining behavior

**Important:** You can also specify specific methods in mocks via `methods = {...}`. Spy is not needed just to limit
mocked methods.

### Behavior

| Parameter                   | Default Value                    | Effect                                                   |
|-----------------------------|----------------------------------|----------------------------------------------------------|
| `spy = false`               | Full mocking                     | Methods don't execute unless specified in `<mockInvoke>` |
| `spy = true`                | Spy mode                         | Real methods execute unless overridden                   |
| `cloneArgsAndResult = true` | Arguments and results are cloned | Avoids side effects from shared references               |

TestConstructorMock
-------------------

### Description

Mocks constructor calls of the specified class.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(TestConstructorMock.List.class)  
public @interface TestConstructorMock {  
    String name() default "";                    // Logical configuration name  
    Class<?> mockClass() default Null.class;     // Class for constructor mocking  
    String mockClassName() default "";           // FQN of class (alternative to mockClass)  
    String[] methods() default {};               // Constructor signatures (optional)  
    boolean spy() default false;                 // Spy for created instances  
    boolean cloneArgsAndResult() default false;  // Clone arguments/results  
}  

```

### When to Use

**Use `@TestConstructorMock` when you need to mock objects that are not Spring beans:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestConstructorMock(mockClass = ExternalApiClient.class)  
public class ApiTest {  
    // new ExternalApiClient(...) will return a mock instead of real object  
}  

```

**Suitable for any object created through a constructor:**

* DTOs and data models
* External service clients
* Utility classes
* Any POJOs created via `new`

### Usage Examples

**Mocking all constructors of a class:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestConstructorMock(mockClass = ExternalApiClient.class)  
public class ApiTest {  
    // new ExternalApiClient(...) will return a mock instead of real object  
}  

```

**Spy for created instances:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestConstructorMock(mockClass = Order.class, spy = true)  
public class OrderTest {  
    // new Order(...) will create a real object but wrap it in a spy  
}  

```

### When to Use mockClassName

`mockClassName` is useful when the class is **not directly accessible** :

* Private classes
* Internal classes
* Classes without direct access in test classpath

```
java  
@TestConstructorMock(mockClassName = "com.example.internal.PrivateClass")  

```

### How It Works

1. Mockito intercepts constructor calls via `Mockito.mockConstruction()`
2. A mock/spy is created instead of the real instance
3. `ConstructorMockAnswer` maintains the mock → original object mapping for spy mode
4. Method calls on the mock are handled via `MockAnswer`

TestStaticMock
--------------

### Description

Mocks static methods of the specified class.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(TestStaticMock.List.class)  
public @interface TestStaticMock {  
    String name() default "";                    // Logical configuration name  
    Class<?> mockClass() default Null.class;     // Class for static mocking  
    String mockClassName() default "";           // FQN of class (alternative to mockClass)  
    String[] methods() default {};               // Specific static methods  
    boolean spy() default false;                 // Spy instead of full mock  
    boolean cloneArgsAndResult() default false;  // Clone arguments/results  
}  

```

### When to Use

**Use `@TestStaticMock` for mocking static methods of utility classes:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestStaticMock(mockClass = IdGenerator.class, methods = {"generate"})  
public class OrderTest {  
    // IdGenerator.generate() will be intercepted  
}  

```

**Typical use cases:**

* Utility classes with static methods (IdGenerator, CryptoUtils, Validators)
* Static factories
* Helper classes for file, network, database operations
* Random value generators (except time)

### Usage Examples

**Mocking a static method of a utility class:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestStaticMock(mockClass = IdGenerator.class, methods = {"generate"})  
public class OrderTest {  
    // IdGenerator.generate() will return value from <mockInvoke>  
}  

```

**Mock for crypto utilities:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestStaticMock(mockClassName = "com.example.utils.CryptoHelper")  
public class CryptoTest {  
    // All static methods of CryptoHelper will be mocked  
}  

```

**Multiple static mocks:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestStaticMock(mockClass = IdGenerator.class, methods = {"generate"})  
@TestStaticMock(mockClass = Validators.class, methods = {"isValid"})  
public class ValidationTest {  
    // Both static classes will be mocked  
}  

```

### When to Use mockClassName

`mockClassName` is useful when the class is **not directly accessible** :

* Private classes
* Internal classes
* Classes without direct access in test classpath

```
java  
@TestStaticMock(mockClassName = "com.example.internal.InternalUtils")  

```

### Features

* Uses `Mockito.mockStatic()` for interception
* Requires ByteBuddy for class instrumentation
* Static mocks are active only within the test scope

⚠️ **Important:** For mocking time, use `@MockJavaTime` and `NowSetter`, not `<mockInvoke>`
for `System.currentTimeMillis()`.

Integration with XML Tests
--------------------------

Mocks are configured via the `<mockInvoke>` element in the test file:

```
xml  
<test type="Case" name="withMock">  
    <bean>orderService</bean>  
    <method>create</method>  
    <request>  
        <value>ORDER-123</value>  
    </request>  
    
    <!-- Expected mock call -->  
    <mockInvoke name="paymentService" method="charge">  
        <args>  
            <value>ORDER-123</value>  
            <value type="BigDecimal">100.00</value>  
        </args>  
        <!-- Expected result -->  
        <result>  
            <return type="PaymentResult">  
                <status>SUCCESS</status>  
            </return>  
        </result>  
        <!-- Or expected exception -->  
        <!-- <result><throw type="PaymentException">Error</throw></result> -->  
    </mockInvoke>  
    
    <response type="Order">  
        <id>ORDER-123</id>  
        <status>CREATED</status>  
    </response>  
</test>  

```

### mockInvoke Inheritance

`<mockInvoke>` elements **inherit up the test hierarchy** . This allows you to move frequently used mocks to the
container level or even root:

```
xml  
<test type="Container">  
    <!-- Common mocks for all tests in this container -->  
    <mockInvoke name="IdGenerator" method="generate">  
        <result>  
            <return type="String">ID-001</return>  
        </result>  
        <result>  
            <return type="String">ID-002</return>  
        </result>  
        <result>  
            <return type="String">ID-003</return>  
        </result>  
    </mockInvoke>  
    
    <test type="Case" name="test1">  
        <bean>service1</bean>  
        <method>run</method>  
        <!-- IdGenerator.generate() will return values from parent mockInvoke -->  
    </test>  
    
    <test type="Case" name="test2">  
        <bean>service2</bean>  
        <method>run</method>  
        <!-- IdGenerator.generate() will return values from parent mockInvoke -->  
    </test>  
</test>  

```

**mockInvoke search** goes from the current test up to the root --- the first match by `name`/`method`/`args` is used.

### mockInvoke Attributes

| Attribute  | Description                                           |
|------------|-------------------------------------------------------|
| `name`     | Mock name (matches `name` in annotation or bean name) |
| `method`   | Method name to intercept                              |
| `disabled` | Skip verification of this call                        |

### Elements Inside mockInvoke

| Element    | Description                                 |
|------------|---------------------------------------------|
| `<args>`   | Expected call arguments (list of `<value>`) |
| `<result>` | Expected result: `<return>` or `<throw>`    |
| `<return>` | Value to return from the mocked method      |
| `<throw>`  | Exception to throw from the mocked method   |

### Result Sequence

If multiple `<result>` are specified, they are returned in sequence:

```
xml  
<mockInvoke name="counter" method="next">  
    <result><return type="Integer">1</return></result>  
    <result><return type="Integer">2</return></result>  
    <result><return type="Integer">3</return></result>  
</mockInvoke>  

```

First call to `counter.next()` returns `1`, second returns `2`, third returns `3`, then the cycle repeats.

### Custom Argument Comparison

For complex types, register custom comparison in `TestSetupModule`:

```
java  
@Configuration  
public class TestConfig {  
    @Bean  
    TestSetupModule testSetupModule() {  
        return new TestSetupModule()  
            .addEqualsForType(MyComplexType.class, (a, b) -> a.getId().equals(b.getId()));  
    }  
}  

```

MockAnswer Mechanism
--------------------

### Interception Lifecycle

```
Method call on mock/spy  
    ↓  
MockAnswer.answer()  
    ├── enabled = false? → callRealMethod()  
    ├── methods != null && method not in list? → callRealMethod()  
    ├── mockInvoke.disabled = true? → callRealMethod()  
    │  
    ├── Search MockInvoke by name/method/args (up the hierarchy)  
    │   ├── Not found + mockCallRealMethodsOnNoData = true → callRealMethod()  
    │   ├── Not found + mockReturnMockOnNoData = true → return deep mock  
    │   └── Not found + both false → error (no expected call)  
    │  
    ├── MockInvoke found  
    │   ├── Record call in test.mockInvoke (if addMockInvoke = true)  
    │   ├── callRealMethod = true? → execute real method + record result  
    │   ├── tryThrowException() → throw exception if specified  
    │   └── Return result (cloned if cloneArgsAndResult = true)  

```

### Global enabled Flag

Interception is active **only** inside `MockAnswer.enable()`:

```
java  
// In TestExecutor.runTest()  
MockAnswer.enable(() -> {  
    // Interception is active here  
    test.executeMethod(...);  // Calls on mock will be intercepted  
});  
// Interception is disabled here  

```

This prevents interception of calls outside the test context.

Common Problems
---------------

### Mock Not Applied

**Cause:** Mismatch of name or type in annotation and configuration.  
**Solution:**

```
java  
// ✅ Correct: name matches @Bean or @Component  
@TestBeanMock(name = "myService")  

// ❌ Error: name doesn't match  
@TestBeanMock(name = "wrongName")  

```

### Mock Name Conflict

**Cause:** Multiple mocks with the same `name`.  
**Solution:**

```
java  
// ✅ Use unique names  
@TestBeanMock(name = "paymentServiceV1")  
@TestBeanMock(name = "paymentServiceV2")  

```

### Arguments Don't Match During Comparison

**Cause:** Argument comparison in `MockInvoke` uses recursive AssertJ comparison.  
**Solution:**

* Ensure argument types match
* For complex objects, register custom comparison in `TestSetupModule`:

```
java  
@Bean  
TestSetupModule testSetupModule() {  
    return new TestSetupModule()  
        .addEqualsForType(MyComplexType.class, (a, b) -> a.getId().equals(b.getId()));  
}  

```

### Static Mock Not Working

**Cause:** Class cannot be overridden (final, system class).  
**Solution:**

* Ensure the class is not `final`
* For system classes, additional ByteBuddy configuration may be required

Recommendations
---------------

1. **Use `name` for explicit identification** --- especially when mocking multiple beans of the same type.
2. **Use spy for selective execution of real methods** --- for example, to throw exceptions under certain conditions.
3. **Enable `cloneArgsAndResult` for mutable objects** --- prevents unexpected data changes between test and mock.
4. **Move common mocks to container level** --- frequently used mocks (IdGenerator, random values) can be defined in a
   parent `Container` for reuse.
5. **Don't use `<mockInvoke>` for time** --- use `@MockJavaTime` and `NowSetter` for time management.
6. **Check `name` uniqueness** --- duplicate names lead to `IllegalArgumentException`.
7. **Use `disabled="true"` in `<mockInvoke>`** for temporarily disabling call verification without removing from XML.
8. **Register custom comparison in `TestSetupModule`** --- use `addEqualsForType()` for complex argument types.
9. **Use `mockClassName` for inaccessible classes** --- private, internal classes, classes without direct access.  
