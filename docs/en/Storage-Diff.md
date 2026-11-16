Storage Changes (`<diff>`)
==========================

[Russian version](../ru/Storage-Diff.md)

`<diff>` is a test expectation: what must change in a storage after the application code
runs. The block works the same for every framework storage — PostgreSQL, Redis, Kafka and your own
ones — so the common rules are collected here, and module specifics are described in the module
documentation.

## When a diff is needed

A test that writes to a database, cache or queue usually checks two things: the method response
(`<response>`) and the storage state. The second one is what `<diff>` is for:

```xml
<diff storage="dataSource">
    <insert key="users/100">
        <id type="Long">100</id>
        <name>Ivan</name>
    </insert>
    <update key="orders/42">
        <status>PAID</status>
    </update>
    <delete key="orders/43"/>
</diff>
```

`storage` is the storage name; for module connections it matches the connection name. One storage
is one `<diff>` block; when several storages changed, there are several blocks. Inside are records:
table rows, Redis keys, elements of your collection.

## How to read a diff

### State before and after

For snapshot storages the framework remembers the content before a test part and after it. Delta
storages (for example, PostgreSQL with a change journal) report their own changes. In both cases
only the difference is written; if nothing changed, the block must not be present.

The diff is calculated from the last recorded state: inside a case each part is compared with the
state after the previous part, and between cases — with the state left by the previous case (unless
the module clears the storage itself). A case that leaves data behind therefore affects the next
case's expectation.

### Operations

* `insert` — the record appeared; the body is the whole value;
* `update` — the record exists and changed; the body contains only the changes, the attributes are
  the complete metadata of the value after the test;
* `delete` — the record or field is gone; the body is empty, only `key` is written.

A record always carries its operation tag: a storage root has no operation to inherit. Even when the
key `orders` is a valid XML tag, the record is written as `<update key="orders">`, not `<orders>`.

For sequences (`List`, an array, `Deque`) the body of an `update` is a snapshot of the new state, not
a set of changes: their elements have no identity of their own. A `Set` is written with added and
removed elements.

### Key, type and attributes

A record key is text (the `key` attribute) when the key body is scalar and an object (the `<key>`
element) when it is structured. The form is decided by the body returned by the key presenter, not
by the declared Java type; for an unregistered type a scalar key is written as text and a structured
key as an object.

Attributes are an ordinary map from a name to a value. They describe the value after the test
rather than a change: an `insert` and an `update` write all attributes of the element, so an
unchanged `ttl` is repeated. An absent attribute means that the value has no such attribute; what an
element lost is visible against the previous state of the same key. `type` is not an ordinary
attribute but a form marker: it is written when the body alone cannot restore the form of the value.

For a text key the order is: `key`, then `type`, then the other attributes. For an object key,
`type` and the attributes stay on the element itself, and `<key>` is a child element.

### Nesting and operation inheritance

The operation is inherited from the nearest operation tag above. Inside an `<update>` a changed
field is written with its own name, an added one is `insert key="..."`, a removed one is
`delete key="..."`. Inside `<insert>` and `<delete>` the body is a snapshot: fields keep their names,
because the operation is inherited. The operation tag is repeated only when the operation differs
from the inherited one or the name cannot be used as an XML tag.

The rules apply recursively: map values by key, object properties by field, collection elements by
element. So the diff shows only what actually changed, at any depth:

```xml
<update key="users/100">
    <profile>
        <city>Moscow</city>
    </profile>
</update>
```

The exception is an element of a collection or a value of a repeated `MultiValueMap` field: it has
no separate `key`, so a `delete` carries the removed value, otherwise it cannot be identified:

```xml
<delete key="header">b</delete>
```

The body of an `insert` and a `delete` is plain data: fields and collection elements keep their
names. An operation tag is needed only for a field whose name cannot be used as an XML tag, and it is
the inherited operation: `<insert key="...">` inside an `insert`, `<update key="...">` inside an
`update`, `<delete key="...">` inside a `delete`.

```xml
<update key="orders/42">
    <status>PAID</status>
    <insert key="paidAt" type="Instant">2026-01-01T10:00:00Z</insert>
</update>
```

Format limitation: an empty value with attributes (for example, an empty string with `ttl`) is
written as a self-closing element and cannot be told from a value without a body.

### Order of fields and elements

The order in the file never has to be maintained by hand: for the same data it is always the same.

* Object fields follow the declaration order (`@JsonPropertyOrder` overrides it); table row fields
  follow the column order.
* Collections and maps that have an order of their own (a list, `LinkedHashSet`, `SortedSet`,
  `EnumSet`, `Deque`, a sorted map, `EnumMap`) are written as they are stored in the source.
* An unordered map (`HashMap`) is sorted by the rendered key; a `HashSet` by the rendered element.
* `TreeMap`/`TreeSet` are considered ordered; if their comparator relies on identity hash, the
  order will not be stable — use `LinkedHashMap`/`LinkedHashSet` or a custom renderer.
* Record order (table rows, Redis keys) follows the snapshot map: `LinkedHashMap`/`SortedMap` keep
  their own order, `HashMap` is sorted by the rendered key.
* Metadata attributes (`ttl`, `score`) are an ordinary map: `LinkedHashMap`/`SortedMap` keep their
  order, while `HashMap` and `Map.of` with 2+ attributes are sorted by name. `type` goes first among
  the metadata attributes, but after `key`.

If the order in an expectation still differs, regenerate the diff instead of fixing it manually.

### What is not in a diff

* data excluded by settings (`ignore`, `excludedFields`);
* changes the storage did not show to the framework (for example, PostgreSQL with disabled
  triggers);
* a change of the concrete class of a non-empty map: it is compared by content and writes no type;
* service data of the module itself.

### How to update expectations

The diff is never written by hand — it is generated from the actual state:

* `-Dintegration.testing.assertion=SaveActualData` — the run writes the actual data into the test
  file;
* the `file` mode of the [IntelliJ IDEA plugin](IDEA-Plugin.md) — comparison in the usual diff dialog
  with a save button.

## How to configure

The operations and the general shape of a diff are fixed: records are always written with the
`insert`, `update` and `delete` tags. The old `integration.testing.storage` section is no longer
needed and can be removed from `application-test.yml`.

Everything else is configured per module:

| What is configured                                                                           | Module     | Where it is described                        |
|----------------------------------------------------------------------------------------------|------------|----------------------------------------------|
| Connections, `defaultSchema`, `columns` (`ignore`, `excludedFields`), `sequenceRestartValue` | PostgreSQL | [Configuration](postgresql/Configuration.md) |
| Serialization schemas, `ignore` by key masks, `multipleDatabases`, `excludedFields`          | Redis      | [Configuration](redis/Configuration.md)      |
| `ignore`, `ignore-inbound`, `excluded-fields`                                                | Kafka      | [Configuration](kafka/Configuration.md)      |

Record key formats also depend on the module: PostgreSQL uses `[schema.]table/pk`, Redis uses `key`
or `db/key` with `multipleDatabases`. An example diff over an arbitrary storage is in
[Records and Headers](kafka/Records-and-Headers.md).

## Custom types

When the standard rules are not enough, register a presenter or a renderer through `TestSetupModule`:

| Method                                                               | What it defines                                             |
|----------------------------------------------------------------------|-------------------------------------------------------------|
| `addDiffKeyPresenter(Class<T>, DiffPresenter<? super T>)`            | representation of a record key                              |
| `addDiffPresenter(Class<T>, DiffPresenter<? super T>)`               | representation of a value: body, type and attributes        |
| `addDiffRenderer(Class<T>, DiffRenderer<? extends T>)`               | body renderer: how the body difference is calculated        |
| `addEqualsForType(Class<T>, BiPredicate<? super T, ? super T>)`      | equality predicate: a change of this type is not a diff     |

Detailed contracts, examples and rules are in
[Diff Extensibility](Storage-Diff-Extensibility.md). For custom Redis structures it is enough to
describe how the value is read and written — see [Redis Extensibility](redis/Extensibility.md).

---
[← Back to home](README.md)
