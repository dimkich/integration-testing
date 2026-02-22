Test Hierarchy
==============

Test Structure
--------------

The framework uses a three-level test hierarchy to organize test scenarios:

```
test type="Container" (root, required, name not required)  
├── test type="Container" (nested container, name required)  
│   ├── test type="Container" (can nest deeper, name required)  
│   └── test type="Case" (test case, name required)  
│       └── test type="Part" (test part, name required)  
└── test type="Case" (test case, name required)  
    └── test type="Part" (test part, name required)  

```

Test Types
----------

| Type        | Class           | Description              | Can Be Parent       | Can Be Child             | `name`                                     |
|-------------|-----------------|--------------------------|---------------------|--------------------------|--------------------------------------------|
| `Container` | `TestContainer` | Logically groups tests   | `Container`, `Case` | Root or `Container` only | Not required for root, required for nested |
| `Case`      | `TestCase`      | Individual test scenario | `Part`              | `Container` only         | **Required**                               |
| `Part`      | `TestPart`      | Step of multi-step test  | No                  | `Case` only              | **Required**                               |

Hierarchy Rules
---------------

### Mandatory Rules

1. **Root is always `Container`**

   ```
   xml  
   <!-- ✅ Correct: root without name -->  
   <test type="Container">  
       <test type="Case" name="test1">...</test>  
   </test>  

   <!-- ❌ Error: root must be Container -->  
   <test type="Case" name="root">  
       <bean>service</bean>  
   </test>  

   ```

2. **`name` is required for all except root**

   ```
   xml  
   <!-- ✅ Correct -->  
   <test type="Container">  
       <test type="Case" name="scenario1">...</test>  
       <test type="Container" name="group">  
           <test type="Case" name="scenario2">...</test>  
       </test>  
   </test>  

   <!-- ❌ Error: Case without name -->  
   <test type="Container">  
       <test type="Case">...</test>  
   </test>  

   ```

3. **`Case` only inside `Container`**

   ```
   xml  
   <!-- ✅ Correct -->  
   <test type="Container">  
       <test type="Case" name="test1">...</test>  
   </test>  

   <!-- ❌ Error: Case cannot be child of Case -->  
   <test type="Case" name="parent">  
       <test type="Case" name="child">...</test>  
   </test>  

   ```

4. **`Part` only inside `Case`**

   ```
   xml  
   <!-- ✅ Correct -->  
   <test type="Case" name="multiStep">  
       <test type="Part" name="step1">...</test>  
       <test type="Part" name="step2">...</test>  
   </test>  

   <!-- ❌ Error: Part cannot be child of Container -->  
   <test type="Container" name="root">  
       <test type="Part" name="step1">...</test>  
   </test>  

   ```

Isolation and Inheritance
-------------------------

### Isolation Model

| Level       | Context Isolation | Settings Inheritance        | State Reset                |
|-------------|-------------------|-----------------------------|----------------------------|
| `Container` | Yes               | From parent containers      | No                         |
| `Case`      | **Yes**           | From all parent `Container` | **Yes** (before each case) |
| `Part`      | **No**            | From parent `Case`          | **No** (share state)       |

### How Inheritance Works

Settings are inherited **top-down** through the hierarchy:

```
xml  
<test type="Container" name="module">  
    <!-- Settings from this container apply to all cases inside -->  
    <init>  
        <!-- Initialization actions -->  
    </init>  
    
    <test type="Case" name="case1">  
        <!-- Before execution: settings from parent applied, state reset -->  
        <bean>service</bean>  
        <method>test1</method>  
    </test>  
    
    <test type="Case" name="case2">  
        <!-- Before execution: settings from parent applied, state reset -->  
        <bean>service</bean>  
        <method>test2</method>  
    </test>  
</test>  

```

### `TestCase` Behavior

Each `TestCase`:

1. **Inherits settings** from all parent `Container` (chain to root)
2. **Gets isolated environment** --- state is reset before execution
3. **Executes independently** from other `Case` --- **does not see** results of previous cases

```
xml  
<test type="Container" name="group">  
    <init>  
        <!-- Initialization settings applied to all cases -->  
    </init>  
    
    <test type="Case" name="createOrder">  
        <!-- Before launch: settings applied, state reset -->  
        <bean>orderService</bean>  
        <method>create</method>  
    </test>  
    
    <test type="Case" name="payOrder">  
        <!-- Before launch: settings applied, state reset -->  
        <!-- Does NOT see results of previous case -->  
        <bean>paymentService</bean>  
        <method>pay</method>  
    </test>  
</test>  

```

### `TestPart` Behavior

`TestPart` inside one `TestCase`:

1. **Inherit settings** from parent `Case` and all `Container`
2. **Do not get state reset** --- share environment with other `Part`
3. **Execute sequentially** --- each `Part` sees changes from previous ones

```
xml  
<test type="Case" name="multiStepScenario">  
    <!-- Case settings applied once before all Part -->  
    <init>  
        <!-- Initialization settings -->  
    </init>  
    
    <test type="Part" name="step1">  
        <!-- Table cleared before step 1 (from init) -->  
        <bean>orderService</bean>  
        <method>create</method>  
        <!-- After execution: new order exists in table -->  
    </test>  
    
    <test type="Part" name="step2">  
        <!-- Table NOT cleared between steps -->  
        <!-- Sees order created in step 1 -->  
        <bean>paymentService</bean>  
        <method>pay</method>  
    </test>  
    
    <test type="Part" name="step3">  
        <!-- Table NOT cleared between steps -->  
        <!-- Sees order and payment from previous steps -->  
        <bean>deliveryService</bean>  
        <method>ship</method>  
    </test>  
</test>  

```

### `Case` vs `Part` Comparison

| Characteristic                 | `TestCase`              | `TestPart`                                    |
|--------------------------------|-------------------------|-----------------------------------------------|
| Context isolation              | Yes                     | No                                            |
| `init` settings inheritance    | From all parents        | From all parents                              |
| State reset before execution   | **Yes**                 | **No**                                        |
| Sees results of previous tests | **No** (full isolation) | **Yes** (from previous `Part` in same `Case`) |
| When to use                    | Independent scenarios   | Multi-step scenarios with dependencies        |

### Example: When to Use `Case`, When to Use `Part`

**Use separate `Case`** for independent scenarios:

```
xml  
<test type="Container" name="orderTests">  
    <init>  
        <!-- Settings applied to all cases -->  
    </init>  
    
    <!-- Each case starts with clean state -->  
    <test type="Case" name="createOrder">  
        <bean>orderService</bean>  
        <method>create</method>  
    </test>  
    
    <test type="Case" name="deleteOrder">  
        <!-- State reset before this case -->  
        <bean>orderService</bean>  
        <method>delete</method>  
    </test>  
    
    <test type="Case" name="findOrder">  
        <!-- State reset before this case -->  
        <bean>orderService</bean>  
        <method>find</method>  
    </test>  
</test>  

```

**Use `Part`** for dependent steps of one scenario:

```
xml  
<test type="Case" name="fullOrderCycle">  
    <init>  
        <!-- Settings applied once before all Part -->  
    </init>  
    
    <!-- Step 1: creates order -->  
    <test type="Part" name="create">  
        <bean>orderService</bean>  
        <method>create</method>  
    </test>  
    
    <!-- Step 2: uses result from step 1 -->  
    <test type="Part" name="pay">  
        <bean>paymentService</bean>  
        <method>pay</method>  
    </test>  
    
    <!-- Step 3: uses results from steps 1-2 -->  
    <test type="Part" name="ship">  
        <bean>deliveryService</bean>  
        <method>ship</method>  
    </test>  
</test>  

```

Test Attributes
---------------

| Attribute  | Type    | Required              | Description                                               |
|------------|---------|-----------------------|-----------------------------------------------------------|
| `type`     | String  | **Yes**               | Test type: `Container`, `Case`, `Part`                    |
| `name`     | String  | **Yes** (except root) | Test name (displayed in JUnit report, used for filtering) |
| `disabled` | Boolean | No                    | Disable test (`true`/`false`, default `false`)            |

### `disabled` Inheritance

If parent is disabled, all child tests are also skipped:

```
xml  
<test type="Container" name="group" disabled="true">  
    <!-- All tests inside will be skipped -->  
    <test type="Case" name="test1">...</test>  
    <test type="Case" name="test2">...</test>  
</test>  

```

Test Fields
-----------

| Field             | Type             | Description                              |
|-------------------|------------------|------------------------------------------|
| `bean`            | String           | Spring bean name for method call         |
| `method`          | String           | Method name to call                      |
| `request`         | List<Object>     | Method arguments (list of `<value>`)     |
| `response`        | Object           | Expected execution result                |
| `mockInvoke`      | List             | Expected mock calls                      |
| `inboundMessage`  | MessageDto       | Inbound message for testing              |
| `outboundMessage` | List<MessageDto> | Expected outbound messages               |
| `custom`          | Map              | Custom test data                         |
| `init`            | List<TestInit>   | Initialization configuration before test |

💡 **Note:** Detailed description of `init` field and available initialization actions is provided in a separate
documentation section.

Examples
--------

### Minimal Test

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<test type="Container">  
    <test type="Case" name="minimal">  
        <bean>service</bean>  
        <method>run</method>  
        <response>ok</response>  
    </test>  
</test>  

```

### Nested Containers

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<test type="Container">  
    <test type="Container" name="moduleA">  
        <test type="Case" name="scenario1">  
            <bean>serviceA</bean>  
            <method>method1</method>  
        </test>  
        <test type="Case" name="scenario2">  
            <bean>serviceA</bean>  
            <method>method2</method>  
        </test>  
    </test>  
    <test type="Container" name="moduleB">  
        <test type="Case" name="scenario1">  
            <bean>serviceB</bean>  
            <method>method1</method>  
        </test>  
    </test>  
</test>  

```

### Mixed Structure

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<test type="Container">  
    <!-- Simple test case -->  
    <test type="Case" name="simpleTest">  
        <bean>service</bean>  
        <method>simple</method>  
        <response>result</response>  
    </test>  
    
    <!-- Container with test group -->  
    <test type="Container" name="complexGroup">  
        <test type="Case" name="multiStep">  
            <test type="Part" name="step1">  
                <bean>service</bean>  
                <method>step1</method>  
            </test>  
            <test type="Part" name="step2">  
                <bean>service</bean>  
                <method>step2</method>  
            </test>  
        </test>  
        <test type="Case" name="anotherSimple">  
            <bean>service</bean>  
            <method>another</method>  
        </test>  
    </test>  
</test>  

```

Test Filtering
--------------

You can run only specific tests by name path from root:

```
java  
@TestFactory  
Stream<DynamicNode> tests() throws Exception {  
    // Run only: Container → "moduleA" → "scenario1"  
    return dynamicTestBuilder.build("tests/all.xml", List.of("moduleA", "scenario1"));  
}  

```

### How Filter Works

Filter selects **path from root** by test names:

```
xml  
<test type="Container">  
    <test type="Container" name="moduleA">  
        <test type="Case" name="scenario1">...</test>  
        <test type="Case" name="scenario2">...</test>  
    </test>  
    <test type="Container" name="moduleB">  
        <test type="Case" name="scenario1">...</test>  
    </test>  
</test>  

```

```
java  
// Run only "moduleA" → "scenario1"  
dynamicTestBuilder.build("tests/all.xml", List.of("moduleA", "scenario1"));  

// Run only "moduleB" → "scenario1"  
dynamicTestBuilder.build("tests/all.xml", List.of("moduleB", "scenario1"));  

// Run all (empty filter = all tests)  
dynamicTestBuilder.build("tests/all.xml");  

```

### Full Test Name

The framework forms full test name from all parents:

```
xml  
<test type="Container" name="moduleA">  
    <test type="Case" name="scenario1">  
        <test type="Part" name="step1">...</test>  
    </test>  
</test>  

```

Full name: `"moduleA", "scenario1", "step1"`  
This name is displayed in JUnit reports and used for filtering.

Why This Hierarchy?
-------------------

| Reason                   | Explanation                                                                                   |
|--------------------------|-----------------------------------------------------------------------------------------------|
| **Logical grouping**     | `Container` allows combining related scenarios (by module, functionality, API)                |
| **Partial execution**    | Can run individual branches via name filter in `DynamicTestBuilder.build(path, allowedNames)` |
| **`Case` isolation**     | Each `Case` executes in isolated context --- no side effects between cases                    |
| **Dependent steps**      | `Part` inside `Case` share state --- convenient for multi-step scenarios                      |
| **Settings inheritance** | `init` and other settings inherited top-down, reducing duplication                            |
| **Readability**          | Hierarchy reflects business scenario structure (group → scenario → step)                      |
| **Reporting**            | JUnit displays hierarchy in reports, simplifying navigation through results                   |
