Stubs in Integration Tests (StubBean, StubConstruction, StubStaticMethods)
==============================================================================

Overview
--------

The framework provides three annotations for configuring stubs based on Mockito:

| Annotation           | Purpose                 | Scope                                                |
|----------------------|-------------------------|------------------------------------------------------|
| `@StubBean`          | Stubbing Spring beans   | Bean instances in the context                        |
| `@StubConstruction`  | Stubbing constructors   | Any objects created through constructors (not beans) |
| `@StubStaticMethods` | Stubbing static methods | Static methods of classes                            |

All annotations:

* Are applied at the test class level
* Support repetition (repeatable)
* Integrate with the `StubCall` system for configuring expected calls in XML

StubBean
------------

### Description

Stubs or creates a spy for a **Spring bean** by name or type.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(StubBean.List.class)
public @interface StubBean {
    String name() default "";                    // Bean name in Spring context  
    Class<?> stubClass() default Null.class;     // Bean type (if name not specified)
    String stubClassName() default "";           // FQN of type (alternative to stubClass)
    String[] methods() default {};               // Specific methods to stub
    boolean spy() default false;                 // Use spy instead of stub
    boolean cloneArgsAndResult() default false;  // Deep clone arguments/results  
}  

```

### When to Use

**Use `@StubBean` when you need to stub a Spring bean:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubBean(name = "paymentService")
public class OrderTest {  
    // paymentService will be replaced with a stub
}  

```

### Usage Examples

**Stubbing a bean by name:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubBean(name = "paymentService")
public class OrderTest {  
    // paymentService will be replaced with a stub
}  

```

**Multiple stubs:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubBean(name = "emailService")
@StubBean(name = "smsService", spy = true)
public class NotificationTest {  
    // emailService --- stub, smsService --- spy
}  

```

**Spy for a bean by type:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubBean(stubClass = UserService.class, spy = true)
public class UserTest {  
    // All beans of type UserService will be spies  
}  

```

### When to Use Spy

`spy = true` is used when you need to **sometimes execute the real method and sometimes not** :

```
java  
@StubBean(name = "paymentService", spy = true)

```

**Typical scenarios:**

* Throwing exceptions under certain conditions
* Partially preserving real logic
* Verifying calls while maintaining behavior

**Important:** You can also specify specific methods in stubs via `methods = {...}`. Spy is not needed just to limit
stubbed methods.

### Behavior

| Parameter                   | Default Value                    | Effect                                                 |
|-----------------------------|----------------------------------|--------------------------------------------------------|
| `spy = false`               | Full stubbing                    | Methods don't execute unless specified in `<stubCall>` |
| `spy = true`                | Spy mode                         | Real methods execute unless overridden                 |
| `cloneArgsAndResult = true` | Arguments and results are cloned | Avoids side effects from shared references             |

StubConstruction
-------------------

### Description

Stubs constructor calls of the specified class.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(StubConstruction.List.class)
public @interface StubConstruction {
    String name() default "";                    // Logical configuration name  
    Class<?> stubClass() default Null.class;     // Class for constructor stubbing
    String stubClassName() default "";           // FQN of class (alternative to stubClass)
    String[] methods() default {};               // Constructor signatures (optional)  
    boolean spy() default false;                 // Spy for created instances  
    boolean cloneArgsAndResult() default false;  // Clone arguments/results  
}  

```

### When to Use

**Use `@StubConstruction` when you need to stub objects that are not Spring beans:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubConstruction(stubClass = ExternalApiClient.class)
public class ApiTest {  
    // new ExternalApiClient(...) will return a stub instead of real object
}  

```

**Suitable for any object created through a constructor:**

* DTOs and data models
* External service clients
* Utility classes
* Any POJOs created via `new`

### Usage Examples

**Stubbing all constructors of a class:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubConstruction(stubClass = ExternalApiClient.class)
public class ApiTest {  
    // new ExternalApiClient(...) will return a stub instead of real object
}  

```

**Spy for created instances:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubConstruction(stubClass = Order.class, spy = true)
public class OrderTest {  
    // new Order(...) will create a real object but wrap it in a spy  
}  

```

### When to Use stubClassName

`stubClassName` is useful when the class is **not directly accessible** :

* Private classes
* Internal classes
* Classes without direct access in test classpath

```
java  
@StubConstruction(stubClassName = "com.example.internal.PrivateClass")

```

### How It Works

1. Mockito intercepts constructor calls via `Mockito.mockConstruction()`
2. A stub/spy is created instead of the real instance
3. `StubConstructionAnswer` maintains the stub → original object mapping for spy mode
4. Method calls on the stub are handled via `StubAnswer`

StubStaticMethods
--------------

### Description

Stubs static methods of the specified class.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(StubStaticMethods.List.class)
public @interface StubStaticMethods {
    String name() default "";                    // Logical configuration name  
    Class<?> stubClass() default Null.class;     // Class for static stubbing
    String stubClassName() default "";           // FQN of class (alternative to stubClass)
    String[] methods() default {};               // Specific static methods  
    boolean spy() default false;                 // Spy instead of full stub
    boolean cloneArgsAndResult() default false;  // Clone arguments/results  
}  

```

### When to Use

**Use `@StubStaticMethods` for stubbing static methods of utility classes:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubStaticMethods(stubClass = IdGenerator.class, methods = {"generate"})
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

**Stubbing a static method of a utility class:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubStaticMethods(stubClass = IdGenerator.class, methods = {"generate"})
public class OrderTest {  
    // IdGenerator.generate() will return value from <stubCall>
}  

```

**Stub for crypto utilities:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubStaticMethods(stubClassName = "com.example.utils.CryptoHelper")
public class CryptoTest {  
    // All static methods of CryptoHelper will be stubbed
}  

```

**Multiple static stubs:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubStaticMethods(stubClass = IdGenerator.class, methods = {"generate"})
@StubStaticMethods(stubClass = Validators.class, methods = {"isValid"})
public class ValidationTest {  
    // Both static classes will be stubbed
}  

```

### When to Use stubClassName

`stubClassName` is useful when the class is **not directly accessible** :

* Private classes
* Internal classes
* Classes without direct access in test classpath

```
java  
@StubStaticMethods(stubClassName = "com.example.internal.InternalUtils")

```

### Features

* Uses `Mockito.mockStatic()` for interception
* Requires ByteBuddy for class instrumentation
* Static stubs are active only within the test scope

⚠️ **Important:** For stubbing time, use `@MockJavaTime` and `NowSetter`, not `<stubCall>`
for `System.currentTimeMillis()`.

Integration with XML Tests
--------------------------

Stubs are configured via the `<stubCall>` element in the test file:

```
xml  
<test type="Case" name="withStub">
    <bean>orderService</bean>  
    <method>create</method>  
    <request>  
        <value>ORDER-123</value>  
    </request>  
    
    <!-- Expected stub call -->
    <stubCall name="paymentService" method="charge">
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
    </stubCall>
    
    <response type="Order">  
        <id>ORDER-123</id>  
        <status>CREATED</status>  
    </response>  
</test>  

```

### stubCall Inheritance

`<stubCall>` elements **inherit up the test hierarchy** . This allows you to move frequently used stubs to the
container level or even root:

```
xml  
<test type="Container">  
    <!-- Common stubs for all tests in this container -->
    <stubCall name="IdGenerator" method="generate">
        <result>  
            <return type="String">ID-001</return>  
        </result>  
        <result>  
            <return type="String">ID-002</return>  
        </result>  
        <result>  
            <return type="String">ID-003</return>  
        </result>  
    </stubCall>
    
    <test type="Case" name="test1">  
        <bean>service1</bean>  
        <method>run</method>  
        <!-- IdGenerator.generate() will return values from parent stubCall -->
    </test>  
    
    <test type="Case" name="test2">  
        <bean>service2</bean>  
        <method>run</method>  
        <!-- IdGenerator.generate() will return values from parent stubCall -->
    </test>  
</test>  

```

**stubCall search** goes from the current test up to the root --- the first match by `name`/`method`/`args` is used.

### Removing Unused stubCalls

When a test file is updated, unused `<stubCall>` declarations are removed from leaf tests: a `Part` or a `Case`
without nested parts. Container tests and cases with parts keep their `<stubCall>` declarations because nested tests
may use them.

A call is considered used if it was matched during test execution (including calls with `disabled="true"`) or if it
was recorded after a real method invocation.

### stubCall Attributes

| Attribute  | Description                                           |
|------------|-------------------------------------------------------|
| `name`     | Stub name (matches `name` in annotation or bean name) |
| `method`   | Method name to intercept                              |
| `disabled` | Skip verification of this call                        |

### Elements Inside stubCall

| Element    | Description                                 |
|------------|---------------------------------------------|
| `<args>`   | Expected call arguments (list of `<value>`) |
| `<result>` | Expected result: `<return>` or `<throw>`    |
| `<return>` | Value to return from the stubbed method     |
| `<throw>`  | Exception to throw from the stubbed method  |

### Result Sequence

If multiple `<result>` are specified, they are returned in sequence:

```
xml  
<stubCall name="counter" method="next">
    <result><return type="Integer">1</return></result>  
    <result><return type="Integer">2</return></result>  
    <result><return type="Integer">3</return></result>  
</stubCall>

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

StubAnswer Mechanism
--------------------

### Interception Lifecycle

```
Method call on stub/spy
    ↓  
StubAnswer.answer()
    ├── enabled = false? → callRealMethod()  
    ├── methods != null && method not in list? → callRealMethod()  
    ├── stubCall.disabled = true? → callRealMethod()
    │  
    ├── Search StubCall by name/method/args (up the hierarchy)
    │   ├── Not found + callRealMethodsOnNoData = true → callRealMethod()
    │   ├── Not found + returnMockOnNoData = true → return deep mock
    │   └── Not found + both false → error (no expected call)  
    │  
    ├── StubCall found
    │   ├── Record call in test.stubCall (if addStubCall = true)
    │   ├── callRealMethod = true? → execute real method + record result  
    │   ├── tryThrowException() → throw exception if specified  
    │   └── Return result (cloned if cloneArgsAndResult = true)  

```

### Global enabled Flag

Interception is active **only** inside `StubAnswer.enable()`:

```
java  
// In TestExecutor.runTest()  
StubAnswer.enable(() -> {
    // Interception is active here  
    test.executeMethod(...);  // Calls on stub will be intercepted
});  
// Interception is disabled here  

```

This prevents interception of calls outside the test context.

Common Problems
---------------

### Stub Not Applied

**Cause:** Mismatch of name or type in annotation and configuration.  
**Solution:**

```
java  
// ✅ Correct: name matches @Bean or @Component  
@StubBean(name = "myService")

// ❌ Error: name doesn't match  
@StubBean(name = "wrongName")

```

### Stub Name Conflict

**Cause:** Multiple stubs with the same `name`.
**Solution:**

```
java  
// ✅ Use unique names  
@StubBean(name = "paymentServiceV1")
@StubBean(name = "paymentServiceV2")

```

### Arguments Don't Match During Comparison

**Cause:** Argument comparison in `StubCall` uses recursive AssertJ comparison.
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

### Static Stub Not Working

**Cause:** Class cannot be overridden (final, system class).  
**Solution:**

* Ensure the class is not `final`
* For system classes, additional ByteBuddy configuration may be required

Recommendations
---------------

1. **Use `name` for explicit identification** --- especially when stubbing multiple beans of the same type.
2. **Use spy for selective execution of real methods** --- for example, to throw exceptions under certain conditions.
3. **Enable `cloneArgsAndResult` for mutable objects** --- prevents unexpected data changes between test and stub.
4. **Move common stubs to container level** --- frequently used stubs (IdGenerator, random values) can be defined in a
   parent `Container` for reuse.
5. **Don't use `<stubCall>` for time** --- use `@MockJavaTime` and `NowSetter` for time management.
6. **Check `name` uniqueness** --- duplicate names lead to `IllegalArgumentException`.
7. **Use `disabled="true"` in `<stubCall>`** for temporarily disabling call verification without removing from XML.
8. **Register custom comparison in `TestSetupModule`** --- use `addEqualsForType()` for complex argument types.
9. **Use `stubClassName` for inaccessible classes** --- private, internal classes, classes without direct access.
