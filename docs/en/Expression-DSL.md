# Expression DSL: `pointcut`, `when`, `await`, `size`

## Introduction

The framework uses a single DSL (domain-specific language) to describe instrumentation declaratively:

* **`pointcut`** — *where* to place the sensor: which classes and methods to instrument;
* **`when`** — *under which conditions* to fire: filtering specific calls at runtime;
* **`await`** — *what to do* when waiting for a future-like object (`@FutureLikeAwait`);
* **`size`** — *how to count* remaining work for services with internal queues (`@QueueLikeAwait`).

Expressions use Java-like syntax and are **compiled lazily by the Janino engine**: on first use (class load or first
call), after which the result is cached. A syntax or type error is detected during instrumentation setup — that is, at
test startup — and results in an exception with a detailed compiler message.

## Contexts and variables

| Attribute                                  | Used by                                                                                                              | Available variables                  | Expected result                                     |
|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------|--------------------------------------|-----------------------------------------------------|
| `pointcut`, `startPointcut`, `endPointcut` | `@OnMethodEnter`, `@OnMethodExit`, `@FutureLikeAwait`, `@MethodCountingAwait`, `@MethodPairAwait`, `@QueueLikeAwait` | `t` (type), `m` (method/constructor) | `boolean`                                           |
| `when`, `startWhen`, `endWhen`             | all of the above                                                                                                     | `o` (object), `a` (arguments)        | `boolean`                                           |
| `await`                                    | `@FutureLikeAwait`                                                                                                   | `o` (the tracked object)             | a statement expression (for example, a method call) |
| `size`                                     | `@QueueLikeAwait`                                                                                                    | `o` (the service)                    | `int` / `Integer`                                   |

Full function references: [`t` and `m`](#pointcut-variables-t-and-m), [`o` and `a`](#when-variables-o-and-a).

## Expression syntax

* Regular Java constructs are supported: `&&`, `||`, `!`, comparisons, arithmetic, the ternary operator, `if`, `for`,
  `instanceof`, casts, and static method calls (for example, `java.lang.Math.max(o.asInt(), 100)`).
* String literals may use single quotes (`'com.example.Service'`) or double quotes (`"com.example.Service"`); inside
  single quotes, escape with `\'`.
* Classes are referenced with a `.class` literal: `o.isSameClass(com.example.Worker.class)`,
  `a.arg(0).isInstance(int.class)`.
* Caching: identical expression text is compiled once and reused by every annotation.
* A compilation error (a typo, a missing method, a wrong type) is an `IllegalArgumentException` carrying the Janino
  message, thrown during instrumentation setup rather than at call time.

---

## `pointcut`: variables `t` and `m`

`pointcut` is evaluated once per class load. There are two variables:

* `t` — `TypeDescriptionWrapper`: the type being instrumented;
* `m` — `MethodDescriptionWrapper`: the method or constructor.

### `t` functions

| Function                                                                      | What it checks                                                     |
|-------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `t.name('com.example.MyService')`                                             | exact fully qualified class name (arrays and primitives supported) |
| `t.simpleName('MyService')`                                                   | simple class name without the package                              |
| `t.inherits('com.example.BaseService')`                                       | the type extends/implements the given type                         |
| `t.ann('com.example.Tracked')`                                                | the class carries the annotation                                   |
| `t.inPackage('com.example')`                                                  | exact package name                                                 |
| `t.packageStartsWith('com.example')`                                          | the package starts with the prefix                                 |
| `t.isPublic()` / `t.isProtected()` / `t.isPackagePrivate()` / `t.isPrivate()` | type access modifier                                               |
| `t.isAbstract()` / `t.isFinal()`                                              | type modifiers                                                     |
| `t.isInterface()` / `t.isAnnotation()` / `t.isEnum()`                         | type kind                                                          |

### `m` functions

| Function                                                                                       | What it checks                                                           |
|------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `m.name('process')`                                                                            | exact method name                                                        |
| `m.isConstructor()`                                                                            | the target is a constructor                                              |
| `m.isMethod()`                                                                                 | the target is a regular method (not a constructor or static initializer) |
| `m.isTypeInitializer()`                                                                        | the target is a static class initializer                                 |
| `m.args(2)`                                                                                    | exactly that many parameters                                             |
| `m.args('java.lang.String', 'int')`                                                            | exact parameter type list in order (FQCN)                                |
| `m.ann('com.example.Tracked')`                                                                 | the method carries the annotation                                        |
| `m.returns('java.lang.String')`                                                                | return type (FQCN)                                                       |
| `m.isStatic()` / `m.isPublic()` / `m.isProtected()` / `m.isPackagePrivate()` / `m.isPrivate()` | method modifiers                                                         |
| `m.isFinal()` / `m.isNative()` / `m.isSynchronized()`                                          | method modifiers                                                         |

### The empty filter rule (important!)

If a `pointcut` describes only a class and contains **no method condition** (`m.*`), **all** methods of that class are
instrumented (and, for an enter hook, all constructors as well). This is convenient for debugging but adds CPU overhead
and a stream of events. Always narrow the method down: `m.name(...)`, `m.isConstructor()`, `m.ann(...)`.

> **Note for `@QueueLikeAwait`:** without a method filter, every method of the type is instrumented, so constructors are
> usually targeted explicitly: `m.isConstructor()`.

### Inheritance

If the target method is declared in a base class and is not overridden in a subclass, then `t.name('...Subclass')` will
not find an injection point. Use `t.inherits('...Base')` so the search traverses the entire inheritance chain.

**Examples:**

```text
t.name('com.example.payment.PaymentService') && m.name('pay')
t.inherits('com.example.worker.BaseWorker') && m.name('process')
t.packageStartsWith('com.example.tasks') && m.ann('com.example.Tracked')
t.name('com.example.Order') && m.isConstructor()
t.inherits('com.example.api.Gateway') && m.name('send') && m.args(2)
```

---

## `when`: variables `o` and `a`

`when` is evaluated **on every call** of an already instrumented method. The pointcut decides where to weave in, while
`when` decides whether a particular call should fire. The variables are:

* `o` — `ObjectWrapper`: the target object (`this`);
* `a` — `ArgsWrapper`: the call arguments.

### `o` functions

| Function                                                                                                                                     | What it does                                                                                  |
|----------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `o.isNull()`                                                                                                                                 | the object is `null`                                                                          |
| `o.isInstance(java.lang.Comparable.class)`                                                                                                   | the object is an instance of the type (primitives supported)                                  |
| `o.isSameClass(com.example.Worker.class)`                                                                                                    | exact class match (subclasses excluded)                                                       |
| `o.get()`                                                                                                                                    | the raw object (`Object`)                                                                     |
| `o.field('status')`                                                                                                                          | reflective field access (including private fields, searching the hierarchy) → `ObjectWrapper` |
| `o.call('size')`, `o.call('substring', 0, 4)`                                                                                                | reflective method call with signature matching (boxing/widening) → `ObjectWrapper`            |
| `o.asString()` / `o.asInt()` / `o.asLong()` / `o.asBoolean()` / `o.asChar()` / `o.asByte()` / `o.asShort()` / `o.asFloat()` / `o.asDouble()` | type cast                                                                                     |
| `o.asList()` / `o.asMap()` / `o.asCollection()` / `o.asIterable()`                                                                           | collection cast                                                                               |

### `a` functions

| Function   | What it does                                                                                                |
|------------|-------------------------------------------------------------------------------------------------------------|
| `a.size()` | the number of arguments                                                                                     |
| `a.arg(0)` | the argument by index → `ObjectWrapper`; out-of-range indices yield an "empty" wrapper (`isNull() == true`) |

### Examples

```text
o.isSameClass(com.example.CriticalWorker.class)
a.arg(0).asString().startsWith('test-')
a.size() == 2 && a.arg(1).asInt() > 1000
o.field('priority').asInt() > 5
!o.isNull() && o.call('isEnabled').asBoolean()
```

### Performance

`when` runs on **every** call of an instrumented method, on an application thread. Reflective operations
(`o.field(...)`, `o.call(...)`) are noticeably more expensive than simple checks. Prefer:

* `isNull()`, `isInstance()`, `isSameClass()`, `a.size()`, `a.arg(...).asString()`;
* moving heavy checks into the pointcut (by type/method) or into the action itself;
* avoiding I/O and database access inside `when`.

---

## `await` and `size`

These attributes also use the DSL but receive **only the `o` variable** — no call arguments are available.

**`await` (`@FutureLikeAwait`)** — a blocking action on the tracked object:

```text
o.call('join')
o.call('get')
o.call('awaitUninterruptibly')
```

**`size` (`@QueueLikeAwait`)** — computing the number of remaining tasks (must return an integer):

```text
o.call('getQueueSize').asInt()
o.call('getPendingTasks').asList().size()
o.field('queue').call('size').asInt()
```

See [Wait-Completion](wait-completion.md) for the strategies themselves.

---

## Special cases and errors

* **`o` may be "empty".** In a constructor enter hook and in a static method the object is not available:
  `o.isNull() == true`. Check this before touching fields or invoking methods.
* **`a.arg(i)` out of range does not throw** — it returns an "empty" wrapper, and a subsequent call (for example,
  `asString()`) yields `null`. Check `a.size()` before chaining.
* **A wrong cast** (`asInt()` on a string) throws. In `when` the exception **propagates into the application method**;
  in `await`/`size` it occurs while waiting. Validate types with `isInstance(...)`/`isSameClass(...)`.
* **`null` values:** `o.field('x').asString()` returns `null` when the field is empty; the next method call on `null`
  throws. Use `isNull()`.
* **A compilation error** is not deferred to runtime: it fails during instrumentation setup with the Janino message
  pointing at the problem.

## Recommendations

1. **Keep the pointcut precise.** Always filter the method (`m.*`) so the whole class is not instrumented.
2. **Keep `when` cheap.** It is a hot path: avoid reflection, I/O, and databases.
3. **Check `null` and sizes** before casting.
4. **For inherited methods** use `t.inherits(...)` instead of `t.name(...)`.
5. **Remember proxies:** for a CGLIB proxy the runtime class name differs from your bean name — prefer
   `t.inherits(...)` and verify that instrumentation does not fire twice.

## See Also

* [Method Hooks](Method-Hooks.md) — `@OnMethodEnter`/`@OnMethodExit` and practical recipes.
* [Wait-Completion System](wait-completion.md) — wait strategies using `pointcut`/`when`/`await`/`size`.
* [Hooks and Converters](Hooks-and-Converters.md) — test lifecycle hooks.
