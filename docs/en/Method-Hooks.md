# Method Hooks: `@OnMethodEnter` and `@OnMethodExit`

## Introduction

Sometimes a test does not need to **wait** for background work — it needs to **see or change** what happens inside
application methods: record a call, inspect arguments, notice an exception, measure duration, or substitute data on the
fly.

The **Method Hooks** system provides a declarative mechanism for that. An annotation with a pointcut expression is
placed on the test class, and the framework uses ByteBuddy to weave invisible advice into matching application
methods. The advice invokes your Java code — a **`MethodAction`** — on method entry or exit. The application code stays
untouched: no test branches, no public methods that exist only for tests.

### Where it fits among the framework mechanisms

| Mechanism                                                                         | Level                       | When it runs                                                   | Purpose                                         |
|-----------------------------------------------------------------------------------|-----------------------------|----------------------------------------------------------------|-------------------------------------------------|
| `BeforeTest` / `AfterTest` / `TestConverter`                                      | test Spring beans           | around test execution (Container/Case/Part)                    | test setup/cleanup, expected-data normalization |
| **`@OnMethodEnter` / `@OnMethodExit`**                                            | application method bytecode | on every matching method call                                  | observation, checks, measurements, emulation    |
| `@FutureLikeAwait`, `@MethodCountingAwait`, `@MethodPairAwait`, `@QueueLikeAwait` | bytecode + waiting          | during the assertion phase, blocks the test until tasks finish | synchronization with asynchronous work          |

Hooks **do not wait** for tasks to finish — they only run an action at call time. To wait for a result, use the
[Wait-Completion system](wait-completion.md). Hooks and wait strategies coexist on the same test class because they are
independent `InstrumentationPlugin`s.

> **See also:** the `pointcut`/`when` DSL used by hooks is shared with the Wait-Completion system. Full references for
> the `t`, `m`, `o`, and `a` functions are given below on this page.

---

## Quick Start

### 1. The service under test

```java
package com.example.payment;

public class PaymentService {
    public Receipt pay(String accountId, BigDecimal amount) {
        // ... business logic ...
        return new Receipt(accountId, amount);
    }
}
```

### 2. The action

A `MethodAction` is a plain Java class with a **public no-args constructor**. It is not created as a Spring bean, so
dependencies are not injected into it (see "MethodAction" below).

```java
package com.example.test;

import io.github.dimkich.integration.testing.execution.hook.MethodAction;

import java.lang.reflect.Executable;
import java.util.Arrays;

public class RecordPaymentAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        HookLog.add("pay(" + Arrays.toString(args) + ")");
    }
}
```

### 3. The test class

```java
package com.example.test;

import io.github.dimkich.integration.testing.execution.hook.OnMethodEnter;
import io.github.dimkich.integration.testing.execution.hook.OnMethodExit;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@OnMethodEnter(
        pointcut = "t.name('com.example.payment.PaymentService') && m.name('pay')",
        action = RecordPaymentAction.class)
@OnMethodExit(
        pointcut = "t.name('com.example.payment.PaymentService') && m.name('pay')",
        action = RecordResultAction.class)
class PaymentServiceTest {
    // ... @IntegrationTesting / DynamicTestBuilder and the scenarios themselves ...
}
```

Hook annotations are meta-annotated with `@IntegrationTesting`, so on a class with `@SpringBootTest` this is enough:
the JUnit extension installs the agent before the Spring context is created, and `PaymentService` methods get
instrumented.

---

## Annotation Reference

Both annotations live in the `io.github.dimkich.integration.testing.execution.hook` package and share the same
attributes.

| Attribute  | Type                            | Required | Default  | Description                                                                                    |
|------------|---------------------------------|----------|----------|------------------------------------------------------------------------------------------------|
| `pointcut` | `String`                        | yes      | —        | DSL expression selecting the classes and methods to instrument. Evaluated once per class load  |
| `when`     | `String`                        | no       | `"true"` | DSL filter expression evaluated **on every method call**; when `false`, no action is performed |
| `action`   | `Class<? extends MethodAction>` | yes      | —        | Action class; instantiated once per annotation during instrumentation setup                    |

### `@OnMethodEnter`

The action runs **at the beginning of a method**, before its body (for a constructor, before the constructor body).
It instruments regular methods, static methods, and constructors.

### `@OnMethodExit`

The action runs **on method exit** — both on a normal return and on an exception. It is **not installed on
constructors**: ByteBuddy cannot invoke advice when a constructor throws before object initialization completes. Use
`@OnMethodEnter` for constructors.

### Shared properties

* **`@Repeatable`** — a test class may declare any number of enter and exit hooks; each is independent and has its own
  action and its own `when`.
* **`@Inherited`** — the annotations are visible on subclasses of the test class.
* **`@Target(TYPE)`**, `@Retention(RUNTIME)` — the annotations apply to classes only.
* **`@IntegrationTesting` meta-annotation** — declaring a hook enables the testing infrastructure; a separate
  `@IntegrationTesting` is not required (and does no harm).

Multiple annotations are applied in declaration order. Do not rely on that order for logically related hooks: if strict
sequencing matters, combine the logic into a single action.

---

## `MethodAction`

```java

@FunctionalInterface
public interface MethodAction {
    void execute(Object target, Executable method, Object[] args,
                 Object returnValue, Throwable thrown) throws Exception;
}
```

### Parameter availability

| Parameter     | Enter, regular method | Enter, static  | Enter, constructor           | Exit, regular method                   | Exit, static             |
|---------------|-----------------------|----------------|------------------------------|----------------------------------------|--------------------------|
| `target`      | the instance          | `null`         | `null` (not constructed yet) | the instance                           | `null`                   |
| `method`      | `Method`              | `Method`       | `Constructor`                | `Method`                               | `Method`                 |
| `args`        | call arguments        | call arguments | constructor arguments        | call arguments                         | call arguments           |
| `returnValue` | `null`                | `null`         | `null`                       | the result, or `null` (void/exception) | the result, or `null`    |
| `thrown`      | `null`                | `null`         | `null`                       | the exception, or `null`               | the exception, or `null` |

More details:

* `method` is a `java.lang.reflect.Executable`: either `Method` or `Constructor`. Check it with
  `method instanceof Constructor`, as the examples do.
* `args` is never `null` (it may be an empty array). **Changes to array elements are visible to the method being
  called** — this allows argument substitution, but requires care.
* `returnValue` in an exit hook can also be `null` on a normal return of a `void` method; use `thrown` to distinguish a
  normal exit from an exception.

### Important action properties

* **An action is not a Spring bean.** The instance is created once per annotation via a no-args constructor
  (`getDeclaredConstructor().newInstance()`). `@Autowired` and constructor injection do not work. Use static
  collectors, an `ApplicationContext` holder, or your own static services to access data.
* **One instance serves all threads.** Actions run on application (SUT) threads, not on the test thread. If an action
  holds state, make it thread-safe (`synchronized`, `ConcurrentHashMap`, `AtomicReference`, `ThreadLocal`).
* **Action exceptions do not break the application.** The advice wraps `action.execute(...)` in `try/catch` and logs the
  error in the `MethodHookAdvice` logger at `FINE` level (`java.util.logging`). The test will **not** fail because of
  it — if an action error must fail the test, record it yourself (for example, in a static collector) and assert on it
  in a scenario.
* **A `when` error is an exception.** Unlike the action, `when` evaluation is outside `try/catch`: an exception (for
  example, a `ClassCastException` from `asInt()` on a non-number) **propagates into the application method**. Keep
  `when` simple and guard checks with `o.isNull()`/`a.size()`.

---

## The `pointcut` and `when` Syntax

Hooks use the framework's shared DSL: `pointcut` (variables `t` and `m`) selects the classes and methods to instrument,
while `when` (variables `o` and `a`) decides which calls run the action.

```text
t.name('com.example.payment.PaymentService') && m.name('pay')
t.inherits('com.example.worker.BaseWorker') && m.name('process')
t.name('com.example.Order') && m.isConstructor()
```

```text
o.isSameClass(com.example.CriticalWorker.class)
a.arg(0).asString().startsWith('test-')
!o.isNull() && o.call('isEnabled').asBoolean()
```

Keep in mind when working with hooks:

* `when` is evaluated on **every** call — keep it cheap;
* an error in `when` propagates into the application method;
* in a constructor enter hook and in a static method the `o` variable is empty (`o.isNull() == true`);
* if the `pointcut` has no method condition (`m.*`), **all** methods of the class are instrumented (plus constructors
  for enter hooks).

The full syntax, all `t`, `m`, `o`, `a` functions, and performance guidance live on the
[Expression DSL](Expression-DSL.md) page.

---

## Execution Model

### How instrumentation is installed

1. The `JunitExtension` JUnit extension (enabled via `@IntegrationTesting`) starts with the highest priority and, *
   *before
   the Spring context is created**, installs the Java agent.
2. `MethodHookPlugin` (an `InstrumentationPlugin` SPI implementation discovered through `ServiceLoader`) checks whether
   the test class declares hooks. If it does, then for each annotation it compiles the `pointcut`, creates
   `PointcutSettings` (the `when` predicate plus an `action` instance), and adds a ByteBuddy advice to the shared agent.
3. Classes loaded **after** that are instrumented on the fly. Classes already loaded before the agent was installed are
   not retransformed unless `@RepeatInstrumentation` is used (see "Troubleshooting").

### Call lifecycle

```text
                        Application method call
                                  │
                                  ▼
                   ┌── @OnMethodEnter advice ──┐
                   │   when == true ?          │
                   │   action.execute(         │
                   │      target, method, args,│
                   │      null, null)          │
                   └────────────┬──────────────┘
                                ▼
                         Method body
                         │          │
                normal   │          │ exception
                return   │          │
                         ▼          ▼
                   ┌── @OnMethodExit advice ───┐
                   │   when == true ?          │
                   │   action.execute(         │
                   │      target, method, args,│
                   │      returnValue, thrown) │
                   └───────────────────────────┘
```

If the action should not run for a particular call, `when` returns `false` and the advice does nothing.

### Multithreading

* An action runs on the thread that executes the application method.
* There is one action instance per annotation — state must be thread-safe.
* Do not block the application thread for long: the action runs synchronously inside the method.

### Call chains

* If one constructor calls another via `this(...)`, the enter hook fires for **each** constructor in the chain. Keep
  that in mind when counting events.
* Nested calls of instrumented methods produce nested `enter/exit` events (like a stack).

---

## Recipes

### 1. Recording calls for assertions

Collect calls into a static thread-safe collector and compare its contents in a scenario:

```java
public final class CallRecorder {
    private static final List<String> calls = Collections.synchronizedList(new ArrayList<>());

    public static void add(String call) {
        calls.add(call);
    }

    public static List<String> drain() {
        synchronized (calls) {
            List<String> copy = new ArrayList<>(calls);
            calls.clear();
            return copy;
        }
    }
}

public class RecordCallAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        CallRecorder.add(method.getName() + "(" + Arrays.toString(args) + ")");
    }
}
```

### 2. Conditional hook

```java

@OnMethodEnter(
        pointcut = "t.inherits('com.example.worker.BaseWorker') && m.name('process')",
        when = "o.isSameClass(com.example.worker.CriticalWorker.class)"
                + " && a.arg(0).asString().startsWith('test-')",
        action = CriticalProcessAction.class)
public class CriticalProcessHookTest {
}
```

### 3. Measuring duration

For an enter/exit pair, use two actions plus a `ThreadLocal`:

```java
public class StartTimerAction implements MethodAction {
    static final ThreadLocal<Long> STARTED = new ThreadLocal<>();

    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        STARTED.set(System.nanoTime());
    }
}

public class StopTimerAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        Long started = StartTimerAction.STARTED.get();
        if (started != null) {
            CallRecorder.add(method.getName() + " took "
                    + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms");
        }
    }
}
```

### 4. Constructors and static methods

```java

@OnMethodEnter(pointcut = "t.name('com.example.Order') && m.isConstructor()",
        action = OrderCreatedAction.class)
@OnMethodEnter(pointcut = "t.name('com.example.Metrics') && m.name('reset')",
        action = MetricsResetAction.class)
public class ConstructorAndStaticHookTest {
}
```

In a constructor enter hook, `target == null` (the object does not exist yet); in a static method action it is `null`
as well.

### 5. Reacting to exceptions

An exit hook receives the exception in the `thrown` parameter:

```java
public class FailureAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        if (thrown != null) {
            CallRecorder.add(method.getName() + " failed: "
                    + thrown.getClass().getSimpleName() + ": " + thrown.getMessage());
        }
    }
}
```

### 6. Transforming arguments

`args` is the actual array passed to the method: changing an element changes what the method body sees. For example,
you can normalize an identifier or substitute a value in test mode. Remember that this changes SUT behavior, so use it
deliberately.

### 7. Combined with Wait-Completion

Hooks and wait strategies are independent plugins and can be declared on the same class:

```java

@FutureLikeAwait(pointcut = "t.name('com.example.AsyncClient') && m.name('send')",
        await = "o.call('get')")
@OnMethodEnter(pointcut = "t.name('com.example.AsyncClient') && m.name('send')",
        action = RecordAsyncCallAction.class)
class AsyncClientTest {
    // ...
}
```

---

## Comparison of Extension Mechanisms

| Criterion                     | `BeforeTest` / `AfterTest` / `TestConverter` | `@OnMethodEnter` / `@OnMethodExit`    | Wait strategies                        |
|-------------------------------|----------------------------------------------|---------------------------------------|----------------------------------------|
| Declared as                   | Spring beans in configuration                | annotations on the test class         | annotations on the test class          |
| Scope                         | every test (Container/Case/Part)             | every matching SUT method call        | background tasks matching the pointcut |
| Trigger point                 | around the test                              | on method entry/exit                  | during verification, blocks the test   |
| Can change SUT data           | no (test data only)                          | yes (through arguments and target)    | no                                     |
| Needs Spring bean access      | yes (it is a bean)                           | no (not a bean)                       | no                                     |
| Sensitive to class load state | no                                           | yes (`when` on every call)            | yes                                    |
| Typical use                   | setup/cleanup, normalization                 | recording, checks, timings, emulation | waiting for completion                 |

---

## Troubleshooting

### The hook does not fire

1. **The class was loaded before the agent was installed.** Add `@RepeatInstrumentation({"com.example"})` to the test
   class — the JUnit extension retransforms all loaded classes whose names start with that prefix.
2. **The pointcut did not match.** Check the class name (FQCN), the method, and the modifiers; for inherited methods use
   `t.inherits(...)`. A `pointcut`/`when` compilation error (Janino) is thrown during instrumentation setup — see the
   startup log.
3. **The method has no bytecode.** `abstract` and `native` methods cannot be instrumented.
4. **It is a constructor but the hook is exit.** Only `@OnMethodEnter` can be installed on constructors.
5. **Missing method filter.** If the `pointcut` has no `m.*` conditions, all methods of the class are instrumented —
   make sure the target method is among them and that extra calls do not interfere.

### Spring proxies and class names

If a bean is wrapped in a CGLIB proxy, the runtime class name differs from your class name. Use
`t.inherits('com.example.MyService')` or the exact implementation class name, and verify that the hook does not fire
twice (on the proxy and on the target object).

### The hook fired twice

* a constructor delegates to another constructor via `this(...)` — the advice fires for each;
* the `@Inherited` annotation plus multiple classes in the hierarchy — check that the hook is not declared twice;
* a missing method filter — the pointcut matched more methods than expected.

### The action "stays silent"

Check `when`: the expression may have returned `false`. If the action threw an exception, it is logged by the
`MethodHookAdvice` logger at `FINE` level — enable it to see the cause.

### A `when` error breaks a business call

`when` is evaluated outside `try/catch`, so an exception propagates into the application method. Simplify the
expression and guard argument access with `a.size()`/`o.isNull()` instead of direct casts.

---

## Limitations and Recommendations

* **Exit hooks and constructors are incompatible** — use enter.
* **Hooks do not wait** — use [Wait-Completion](wait-completion.md) to synchronize with asynchronous work.
* **An action is not a Spring bean** — do not expect dependency injection.
* **Keep actions minimal**: no long operations, network, or database access — you are on an application thread.
* **Use precise pointcuts**: always filter the method so the whole class is not instrumented.
* **Thread safety**: one action instance, many threads.
* **Ordering of multiple hooks** is not guaranteed; use a single action when strict sequencing matters.
* **Action errors are hidden by default** (level `FINE`) — if they matter, record them in your own structures and
  assert on them in scenarios.

---

## Runnable Example

A complete working example covering every case (enter/exit, `when`, constructors, static methods, exceptions) lives in
the module tests:

* `integration-testing/src/test/java/io/github/dimkich/integration/testing/execution/hook/HookTest.java`
* `integration-testing/src/test/java/io/github/dimkich/integration/testing/execution/hook/EnterAction.java`
* `integration-testing/src/test/java/io/github/dimkich/integration/testing/execution/hook/ExitAction.java`
* `integration-testing/src/test/resources/execution/hook/hook.xml`

---

## See Also

* [Expression DSL](Expression-DSL.md) — syntax and reference for `pointcut`/`when`.
* [Wait-Completion System](wait-completion.md) — wait strategies built on the shared DSL.
* [Hooks and Converters](Hooks-and-Converters.md) — test lifecycle hooks (`BeforeTest`, `AfterTest`,
  `TestConverter`).
* [Kafka Error Handling](kafka/Error-Handling.md) — forcing an uncommitted offset to commit with an
  exit hook.
* [Test Initialization](Initialization.md) — environment setup before scenarios.
