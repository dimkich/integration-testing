TestSetupModule
===============

Overview
--------

`TestSetupModule` is the central configuration module of the framework for registering types, aliases, custom
serializers, and other serialization/deserialization settings for tests.

Purpose
-------

The module solves the following tasks:

| Task                           | Description                                                     |
|--------------------------------|-----------------------------------------------------------------|
| **Register polymorphic types** | Maps Java classes to logical names for XML/JSON                 |
| **Configure aliases**          | Allows using short names instead of fully qualified class names |
| **Customize Jackson**          | Add modules, filters, mixins for serialization                  |
| **Configure cloning**          | Configure cloning behavior for specific types                   |
| **Custom comparison**          | Register custom equality predicates for types                   |

Creating a Module
-----------------

```
java  
@Bean  
TestSetupModule testSetupModule() {  
    return new TestSetupModule()  
        .addSubTypes(MyDto.class, "MyDto")  
        .addAlias(ByteArrayResource.class, "Resource")  
        .addJacksonModule(new MyCustomModule());  
}  

```

Configuration Methods
---------------------

### Type Registration

| Method                                                   | Description                                           | Example                                                          |
|----------------------------------------------------------|-------------------------------------------------------|------------------------------------------------------------------|
| `addSubTypes(Class<?> subType, String name)`             | Registers a subtype with an explicit name             | `.addSubTypes(MyDto.class, "MyDto")`                             |
| `addSubTypes(Class<?>... classes)`                       | Registers subtypes with default names (simple name)   | `.addSubTypes(MyDto1.class, MyDto2.class)`                       |
| `addSubTypes(String packageName)`                        | Scans a package and registers all classes as subtypes | `.addSubTypes("com.example.dto")`                                |
| `addSubTypes(String packageName, Set<Class<?>> exclude)` | Scans a package with exclusions                       | `.addSubTypes("com.example.dto", Set.of(Excluded.class))`        |
| `addSubTypes(JsonSubTypes jsonSubTypes)`                 | Registers subtypes from `@JsonSubTypes` annotation    | `.addSubTypes(Selector.class.getAnnotation(JsonSubTypes.class))` |

### Type Aliases

| Method                                     | Description                         | Example                                          |
|--------------------------------------------|-------------------------------------|--------------------------------------------------|
| `addAlias(Class<?> subType, String alias)` | Adds an alternative name for a type | `.addAlias(ByteArrayResource.class, "Resource")` |

### Jackson Modules and Filters

| Method                                                            | Description                   | Example                                   |
|-------------------------------------------------------------------|-------------------------------|-------------------------------------------|
| `addJacksonModule(com.fasterxml.jackson.databind.Module module)`  | Adds a Jackson module         | `.addJacksonModule(new JavaTimeModule())` |
| `addJacksonFilter(String id, PropertyFilter filter)`              | Adds a Jackson filter         | `.addJacksonFilter("filterId", myFilter)` |
| `setHandlerInstantiator(HandlerInstantiator handlerInstantiator)` | Sets the handler instantiator | `.setHandlerInstantiator(myHandler)`      |

### Cloning Configuration

| Method                                                              | Description                           | Example                                                                     |
|---------------------------------------------------------------------|---------------------------------------|-----------------------------------------------------------------------------|
| `clonerFieldAction(Class<?> type, String field, CopyAction action)` | Action for a specific field           | `.clonerFieldAction(Throwable.class, "stackTrace", CopyAction.ORIGINAL)`    |
| `clonerFieldAction(Field field, CopyAction action)`                 | Action for a field via reflection     | `.clonerFieldAction(field, CopyAction.ORIGINAL)`                            |
| `clonerTypeAction(Class<?> type, CopyAction action)`                | Action for all fields of a type       | `.clonerTypeAction(SecureRandom.class, CopyAction.ORIGINAL)`                |
| `clonerTypeAction(Predicate<Class<?>> type, CopyAction action)`     | Action for types matching a predicate | `.clonerTypeAction(Throwable.class::isAssignableFrom, CopyAction.ORIGINAL)` |

### Custom Comparison

| Method                                                                      | Description                                | Example                                                     |
|-----------------------------------------------------------------------------|--------------------------------------------|-------------------------------------------------------------|
| `addEqualsForType(Class<T> type, BiPredicate<? super T, ? super T> equals)` | Registers an equality predicate for a type | `.addEqualsForType(SecureRandom.class, (sr1, sr2) -> true)` |

Configuration Examples
----------------------

### Basic Configuration

```
java  
@Configuration  
public class TestConfig {  
    
    @Bean  
    TestSetupModule testSetupModule() {  
        return new TestSetupModule()  
            // Register DTOs  
            .addSubTypes(MyDto.class, "MyDto")  
            .addSubTypes(AnotherDto.class, "AnotherDto")  
            
            // Aliases  
            .addAlias(ByteArrayResource.class, "Resource")  
            
            // Jackson modules  
            .addJacksonModule(new JavaTimeModule());  
    }  
}  

```

### Advanced Configuration

```
java  
@Configuration  
public class AdvancedTestConfig {  
    
    @Bean  
    TestSetupModule testSetupModule() {  
        return new TestSetupModule()  
            // Package scanning  
            .addSubTypes("com.example.dto", Set.of(ExcludedDto.class))  
            
            // Registration from annotation  
            .addSubTypes(Selector.class.getAnnotation(JsonSubTypes.class))  
            
            // Jackson modules  
            .addJacksonModule(new JavaTimeModule())  
            .addJacksonModule(new StoreLocationModule())  
            
            // Mixins  
            .addJacksonModule(new SimpleModule()  
                .setMixInAnnotation(Throwable.class, ThrowableMixIn.class)  
                .setMixInAnnotation(SecureRandom.class, SecureRandomMixIn.class))  
            
            // Cloning configuration  
            .clonerTypeAction(Throwable.class::isAssignableFrom, CopyAction.ORIGINAL)  
            .clonerTypeAction(SecureRandom.class, CopyAction.ORIGINAL)  
            
            // Custom comparison  
            .addEqualsForType(SecureRandom.class, (sr1, sr2) -> true);  
    }  
}  

```

### Example: Cloning a Field via Reflection

```
java  
@Configuration  
public class TestConfig {  
    
    @Bean  
    TestSetupModule testSetupModule() throws NoSuchFieldException {  
        Field stackTraceField = Throwable.class.getDeclaredField("stackTrace");  
        
        return new TestSetupModule()  
            // Configure cloning for a specific field  
            .clonerFieldAction(stackTraceField, CopyAction.ORIGINAL)  
            
            // Configure cloning for all fields of a type  
            .clonerTypeAction(Throwable.class, CopyAction.ORIGINAL);  
    }  
}  

```

Pre-registered Types
--------------------

The framework already registers the following common types via `CommonFormatConfig`:

### Test Types

| Class           | Name        |
|-----------------|-------------|
| `TestContainer` | `Container` |
| `TestCase`      | `Case`      |
| `TestPart`      | `Part`      |

### Primitives and Wrappers

| Class                                                     | Name        |
|-----------------------------------------------------------|-------------|
| `String`, `Integer`, `Long`, `Double`, `Float`, `Boolean` | Simple name |
| `BigDecimal`, `BigInteger`                                | Simple name |
| `boolean`, `int`, `long`, `double`, `float`               | Simple name |

### Collections

| Class                                   | Name                  |
|-----------------------------------------|-----------------------|
| `ArrayList`, `LinkedHashMap`, `TreeMap` | Simple name           |
| `LinkedHashSet`, `TreeSet`              | Simple name           |
| `byte[]`, `int[]`, `long[]`, `double[]` | Simple name with `[]` |

### Date and Time

| Class                                     | Name        |
|-------------------------------------------|-------------|
| `LocalTime`, `LocalDate`, `LocalDateTime` | Simple name |
| `ZonedDateTime`, `Instant`, `Date`        | Simple name |

### Special Types

| Class                    | Name/Alias                                 |
|--------------------------|--------------------------------------------|
| `SecureRandom`           | `SecureRandom`                             |
| `Resource`               | `Resource` (alias for `ByteArrayResource`) |
| `UUID`, `Class`          | Simple name                                |
| `Throwable` and subtypes | Simple name                                |

💡 **Tip:** You don't need to register these types again --- they are already available for use in tests.

How It Works
------------

### 1. Type Registration

```
java  
// In configuration  
.addSubTypes(MyDto.class, "MyDto")  

// In XML  
<response type="MyDto">  
    <id>1</id>  
    <name>test</name>  
</response>  

// In JSON  
{  
    "type": "MyDto",  
    "id": 1,  
    "name": "test"  
}  

```

### 2. Serialization

During serialization, the framework uses the **short name** of the type registered in `TestSetupModule`:

```
java  
// If MyDto.class is registered as "MyDto"  
// XML/JSON will always have type="MyDto"  
// Fully qualified names are not used  

```

### 3. Deserialization

During deserialization, the framework looks up the type by its short name:

```
1. Jackson reads attribute/field "type": "MyDto"  
2. TypeResolverFactory.resolve("MyDto") → MyDto.class  
3. Jackson deserializes into MyDto.class  

```

⚠️ **Important:** If a type is not registered, deserialization will fail. Using fully qualified names in XML/JSON does
not work --- the framework always expects short names.

Interaction with Other Components
---------------------------------

| Component                  | Interaction                                                  |
|----------------------------|--------------------------------------------------------------|
| `TypeResolverFactory`      | Collects data from all `TestSetupModule` instances           |
| `TestTypeResolverBuilder`  | Uses `TypeResolverFactory` to resolve types                  |
| `SharedTypeNameIdResolver` | Created by `TypeResolverFactory` based on module settings    |
| `IntegrationTestConfig`    | Imports configuration, creates `Cloner` with module settings |
| `XmlConfig` / `JsonConfig` | Use `TestTypeResolverBuilder` for polymorphism               |
| `CommonFormatConfig`       | Provides a base `TestSetupModule` with standard types        |

CopyAction for Cloning
----------------------

| Value                     | Description                            |
|---------------------------|----------------------------------------|
| `CopyAction.ORIGINAL`     | Use the original object (do not clone) |
| `CopyAction.DEEP_COPY`    | Deep cloning                           |
| `CopyAction.SHALLOW_COPY` | Shallow cloning                        |

Common Problems
---------------

### Type Name Conflict

**Cause:** Two types are registered with the same logical name.  
**Solution:**

```
java  
// ❌ Error: two types with name "Dto"  
.addSubTypes(FirstDto.class, "Dto")  
.addSubTypes(SecondDto.class, "Dto")  

// ✅ Correct: unique names  
.addSubTypes(FirstDto.class, "FirstDto")  
.addSubTypes(SecondDto.class, "SecondDto")  

```

### Type Not Found During Deserialization

**Cause:** Type is not registered in `TestSetupModule`.  
**Solution:**

```
java  
// Add type registration  
.addSubTypes(MyMissingType.class, "MyMissingType")  

// ❌ Does not work: using fully qualified name in XML  
<response type="com.example.MyMissingType">...</response>  

```

⚠️ **Important:** The framework always uses short type names. Fully qualified names in XML/JSON are not supported.

### Cloning Does Not Work for a Type

**Cause:** Cloning action is not configured for the type.  
**Solution:**

```
java  
// Configure cloning  
.clonerTypeAction(MyType.class, CopyAction.DEEP_COPY)  

// Or for a hierarchy  
.clonerTypeAction(MyBaseType.class::isAssignableFrom, CopyAction.DEEP_COPY)  

```

Recommendations
---------------

1. **Register all polymorphic types** --- if a type is used in `response` or `request` with a `type` attribute, it must
   be registered.
2. **Check name uniqueness** --- the framework will throw an exception if a type name is duplicated.
3. **Use package scanning for DTOs** --- instead of manually registering each type:

   ```
   java  
   .addSubTypes("com.example.dto")  

   ```

4. **Configure cloning for stateful types** --- especially for `Throwable`, `Resource`, `InputStream`.
5. **Remember:** Type names must be unique across all registered modules.  
