# Deterministic Collections — `@DeterministicCollections`

An integration-test annotation that makes collection iteration order predictable. It replaces
order-sensitive JDK factories and `HashMap`/`HashSet` constructors with ordered analogues
(`LinkedHashMap`/`LinkedHashSet`) directly in the application bytecode.

## Why it is needed

Inside the JDK, `Map.of`, `Map.ofEntries`, `Map.copyOf`, `Set.of`, `Set.copyOf` and
`Collectors.toUnmodifiableMap/Set` use `ImmutableCollections` with a **random SALT per JVM run**.
As a result, iteration order changes between runs even for identical arguments:

```java
Map.of("a", 1, "b", 2).keySet().forEach(System.out::println); // a, b or b, a
```

`List.of` and `Stream.toList` are not affected — they are index-based, so the annotation leaves
them untouched.

`HashMap`/`HashSet` also produce unpredictable order for keys with identity hash codes, and
`Collectors.toSet`/`toMap`/`groupingBy` depend on the internal table.

When XML snapshots or `dataStorageDiff` contain such collections, tests start to flake.

## Quick start

```java
@DeterministicCollections("com.example.myapp")
@SpringBootTest
public class MyIntegrationTest {
    // ...
}
```

`value()` is a mandatory list of fully qualified class/package prefixes. Only classes whose
names start with one of the prefixes are instrumented.

## What is replaced

| JDK API                                          | Result                       |
|--------------------------------------------------|------------------------------|
| `Map.of(...)`, `Map.ofEntries(...)`              | unmodifiable `LinkedHashMap` |
| `Map.copyOf(map)`                                | unmodifiable ordered copy    |
| `Set.of(...)` (including varargs)                | unmodifiable `LinkedHashSet` |
| `Set.copyOf(collection)`                         | unmodifiable ordered copy    |
| `Collectors.toSet()`                             | `LinkedHashSet`              |
| `Collectors.toUnmodifiableSet()`                 | unmodifiable `LinkedHashSet` |
| `Collectors.toMap(k, v[, merge])`                | `LinkedHashMap`              |
| `Collectors.toUnmodifiableMap(...)`              | unmodifiable `LinkedHashMap` |
| `Collectors.groupingBy(...)` (1 and 2 arguments) | `LinkedHashMap`              |
| `new HashMap(...)`                               | `new LinkedHashMap(...)`     |
| `new HashSet(...)`                               | `new LinkedHashSet(...)`     |

## What is preserved

Only the **iteration order** changes. The JDK contract remains the same:

- `Map.of`, `Set.of`, `copyOf`, `toUnmodifiable*` stay **unmodifiable**;
- `toSet`, `toMap`, `groupingBy`, `new HashMap`, `new HashSet` stay **mutable**;
- `null` in `Map.of`/`Set.of`/`copyOf` still throws `NullPointerException`;
- duplicate keys/elements still throw `IllegalArgumentException`;
- `Collectors.toMap` without a merge function still throws `IllegalStateException` on a duplicate.

This matters: if a test allows `map.put(...)` where production `Map.of` would throw
`UnsupportedOperationException`, the test would hide a real defect.

## Disabling groups

All replacements are enabled by default. To temporarily disable a group (for diagnostics or
because constructor rewriting conflicts with a library), use `disabled`:

```java
@DeterministicCollections(
        value = "com.example.myapp",
        disabled = DeterministicCollections.Feature.HASH_COLLECTIONS)
```

Available groups:

| Group              | What it disables                                                         |
|--------------------|--------------------------------------------------------------------------|
| `MAP_OF`           | `Map.of` and `Map.ofEntries`                                             |
| `COPY_OF`          | `Map.copyOf` and `Set.copyOf`                                            |
| `SET_OF`           | `Set.of`                                                                 |
| `COLLECTORS`       | `toSet`, `toUnmodifiableSet`, `toMap`, `toUnmodifiableMap`, `groupingBy` |
| `HASH_COLLECTIONS` | `new HashMap` and `new HashSet`                                          |

Disabling `MAP_OF`, `SET_OF`, `COPY_OF` or `COLLECTORS` brings non-deterministic order back —
use it for diagnostics only. The main practical case is disabling `HASH_COLLECTIONS` when
constructor rewriting conflicts with a particular library.

## Class loading and re-instrumentation

The agent is installed before the Spring context is created, so all application beans and classes
are loaded already instrumented. Already loaded classes (including the test class itself) are
retransformed during agent installation.

If another tool reloads classes later, add `@RepeatInstrumentation` with the same prefix:

```java
@DeterministicCollections("com.example.myapp")
@RepeatInstrumentation({"com.example.myapp"})
```

## Limitations

- Method references such as `Map::ofEntries` are compiled into `invokedynamic` and are not
  intercepted. The same applies to `HashMap::new` and `HashSet::new`.
- Anonymous subclasses like `new HashMap<>() {{ ... }}` remain `HashMap` subclasses: `NEW` creates
  the anonymous class and its `super()` call is not rewritten.
- `ConcurrentHashMap`, `Hashtable`, `WeakHashMap` and `IdentityHashMap` are left untouched:
  changing their implementation would break their semantics.
- `TreeMap`, `TreeSet`, `EnumMap`, `EnumSet`, `LinkedHashMap` and `LinkedHashSet` are already
  deterministic and need no replacement.
- `ConcurrentHashMap` iteration order remains non-deterministic; sort the data before serialization
  if needed.

## Recommendations

1. Put only application packages in `value()`, not the whole classpath: instrumentation scans method
   bodies of every matching class.
2. Do not disable `MAP_OF`/`SET_OF`/`COPY_OF`/`COLLECTORS` for long — that brings back the problem
   the annotation was created to solve.
3. Verify not only order but also the contract: mutability and `null`/duplicate behaviour stay
   JDK-compatible.
