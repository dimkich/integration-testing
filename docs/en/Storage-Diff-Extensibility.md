Diff Extensibility
==================

[Russian version](../ru/Storage-Diff-Extensibility.md)

This page is for cases where the standard `<diff>` rules are not enough: your type has a special key,
value or body. How to read and write a diff is described in
[Storage Changes](Storage-Diff.md); this page covers the extension API.

## Record key — `addDiffKeyPresenter`

A key presenter returns the body of the key, and the form is decided by that body using the same
type detector as for a value:

* a scalar body (`String`, a number, an enum, `UUID`, `BigDecimal`, a type with `@JsonValue`) is
  written as a single line by the serializer of the current format;
* a structured body (an object, a `Map`, a `Set`, a `Collection`) is written as an object: type,
  attributes and body.

So one method covers both cases. For an unregistered type the form is detected automatically: a
scalar type is written as text, a structured type as an object projected by the Jackson rules the
same way as a value.

### Text key

If the key should be shortened to a line, return a scalar from the presenter:

```java
@Bean
TestSetupModule testSetupModule() {
    return new TestSetupModule()
            .addDiffKeyPresenter(MarketInstrumentKey.class,
                    (key, attributes) -> key.getExchange() + "," + key.getClassCode() + "," + key.getTicker());
}
```

In a diff this gives a readable key:

```xml
<insert key="MOEX,TQCB,SBER">...</insert>
```

Requirements for the string: stable between runs, free of line breaks and control characters,
unique within one map (for records — within the record set of a storage). The framework fails with
a clear error when two different keys of one map render the same string. A text key has no type and
attributes: the `type` and `attributes` of a presenter belong to the object form only.

### Object key

A structured key is an object without any configuration: an unregistered type is projected by the
Jackson rules the same way as a value. The key is written as a snapshot: type, attributes and body.
A text key stays the `key` attribute, an object key is a `<key>` child; in the usual case the two
forms do not collide:

```xml
<insert type="MD">
    <key type="MarketInstrumentKey">
        <exchange>MOEX</exchange>
        <classCode>TQCB</classCode>
        <ticker>SBER</ticker>
    </key>
    <value>
        <closePrice type="BigDecimal">259.43</closePrice>
    </value>
</insert>
```

`<value>` contains the value body; when there is no value (a `delete` or an attributes-only value),
`<value>` is absent:

```xml
<delete type="MD">
    <key type="MarketInstrumentKey">
        <exchange>MOEX</exchange>
        <classCode>TQCB</classCode>
        <ticker>SBER</ticker>
    </key>
</delete>
```

An element without the `key` attribute whose body is exactly a `key` field (or `key` and `value`)
is structurally the same as an object key. When other fields follow the `<key>`, they stay ordinary
body fields.

What is important to know:

* a structured key is projected by the Jackson rules by default, the same as a value; properties
  serialized as XML attributes become key attributes;
* `addDiffKeyPresenter(type, presenter)` defines a custom key body: a scalar body makes the key
  text, a structured body makes it an object with a custom projection. Key and value presenters
  are looked up separately: a value presenter of the same type is **not** applied to the key
  automatically;
* a key is a snapshot: its elements are not changes, fields are written by name. A field whose name
  cannot be used as an XML tag is still written as `<insert key="...">`; that is a wrapper of the
  name, not an operation;
* the key is compared by its representation — type, attributes and body — not by
  `equals`/`hashCode`/`toString` of the domain class. A mutable key and a key without a compact
  `toString()` work; two keys with the same representation are an error;
* object keys of records are sorted deterministically by type, attributes and body;
  `LinkedHashMap`/`LinkedHashSet` keep their insertion order; `TreeMap`/`TreeSet` keep their
  comparator order even when the keys are objects. If the comparator relies on identity hashes, the
  order is not stable: use `LinkedHashMap`/`LinkedHashSet` or a custom renderer;
* `MultiValueMap` (for example, Kafka headers) requires a text key: repeated elements are written
  by field name, so the presenter of such a key has to return a scalar body;
* if the key appears in a test `<request>`/`<response>`, register it as a regular subtype
  (`addSubTypes`).

## Value representation — `addDiffPresenter`

A presenter describes a value with the `present` method: it adds metadata to
`DiffAttributes.Builder` and returns the raw body. The builder validates names and values, and sets
the attribute order. The presenter overrides `type` when needed, and `TypeNames.typeOf` gives the
type name of the value. The difference itself is always built by the framework, so a presenter
never compares values and never decides between `insert` and `update`.

```java
public class RedisEntryPresenter implements DiffPresenter<RedisEntry> {

    @Override
    public Object present(RedisEntry entry, DiffAttributes.Builder attributes) {
        attributes.addAttributes(ttl(entry));
        return entry.getData();
    }

    @Override
    public String type(RedisEntry entry, DiffValue body, TypeNames types) {
        return types.typeOf(entry.getData());
    }

    private Map<String, String> ttl(RedisEntry entry) {
        if (entry.getTtl() == null) {
            return Map.of();
        }
        return Map.of("ttl", entry.getTtl().toString());
    }
}
```

Registration is a regular module method:

```java
@Bean
TestSetupModule testSetupModule() {
    return new TestSetupModule()
            .addDiffPresenter(RedisEntry.class, new RedisEntryPresenter());
}
```

What is important to know:

* `present` returns the body of the value and puts the attributes into
  `DiffAttributes.Builder`. The body is ordinary data — a map, a collection, an array or a scalar —
  and the framework diffs it by the same rules as any other value. An object with properties is
  presented as a map; the default presenter does it automatically;
* a body is a value too: when its type has its own presenter, that presenter is applied and its
  attributes are added to the element attributes. For example, `RedisEntryPresenter` returns
  `RedisZSet`, and `RedisZSetPresenter` turns it into a `member → score` map; without this a ZSET
  would lose its members. The element type comes from the root presenter (`type`), not from the
  body presenter;
* attributes are a `Map<String, String>` from a name to a value. `addAttribute` validates the name
  and the value immediately; the same value added twice is ignored, different values for one name
  throw `IllegalStateException`. `addAttributes` accepts a ready map: `LinkedHashMap`/`SortedMap`
  set the order, an unordered map with 2+ attributes is sorted by name; an absent attribute is
  simply not added;
* `type` by default returns the type of the value itself. The built-in container presenter (`Map`,
  `Set`, `Collection`) writes no type for a non-empty map, because its fields describe the
  structure; a map without changed fields keeps its type. A collection, set or array always writes
  its type: their elements are unnamed, and the body alone does not tell a list from an array. A
  custom presenter that returns a map keeps the type by default — drop it in `type` when the fields
  are self-describing. Override it only when the type of the element has to be the type of the body:
  for example, a Redis value is written with the type of its payload, and a presenter of a base type
  keeps the name of the base type;
* `type` is a part of the identity of a value: when the type before and after differs, the element is
  written as an `update`, even when the body did not change. The type is compared by the value
  returned by the presenter, not by the Java class, so `BaseValue`/`DerivedValue` with one presenter
  stay one type. A matching user `addEqualsForType` predicate is stronger than this check;
* for a removed value only `type` is called: the body and the attributes are not written;
* a body without data (`null`) is written as attributes only, for example `<member score="10.5"/>`;
* attribute names are simple XML names without a namespace; `key`, `type`, `nil` and `xmlns` are
  reserved by the format and cannot be presenter attributes;
* when several registered presenters match (for example, one for `Set` and one for `Collection`),
  the most specific type wins: a subtype beats its supertype and the registration order does not
  matter. For the same type a user module wins: built-in registrations are applied first;
* the difference of the body is built by a renderer selected by the body type, so a presenter may
  describe a value without reshaping it.

Without a presenter the Jackson rules apply: object properties, `@JsonInclude`,
`@JacksonXmlProperty(isAttribute = true)`. For `Optional`, `AtomicReference` and
`AtomicMarkableReference` the built-in presenter writes the referenced value (the mark of a
markable reference does not participate, matching its comparison preset). The default presenter
does not honour `@JsonInclude(NON_DEFAULT)` and custom filters: such a value needs its own
`DiffPresenter`.

## Body rendering — `addDiffRenderer`

A presenter describes the body of a value, and a renderer builds the difference of the body. The
built-in renderers cover `Map`, `Set`, `MultiValueMap`, collections, arrays and scalars; a body of
another shape needs its own renderer:

```java
.addDiffRenderer(Tags.class, new TagsDiffRenderer());
```

The renderer is selected by the type of the final body returned by the presenter (after all body
presenters), not by the type of the value. The default presenter projects an object into a map, so
a custom body needs a presenter that returns that body. `addDiffRenderer` alone is not enough.
The renderer must accept every subtype of the registered type: a renderer typed for `HashMap` and
registered for `Map.class` compiles but fails on `LinkedHashMap`.

`DiffRenderer<T>` receives the body before and after the test and returns a `DiffValue`:

* `before` — the body before the test produced by the presenter of the previous value. It may be
  `null` when there was no value, or have another type when the value changed its shape. The
  renderer itself decides how to treat a body of another shape: the built-in containers (`Map`,
  `Set`, `MultiValueMap`) compare it with an empty container, so every element of the new body is
  written as `insert`; a scalar is written as its value and a sequence as a snapshot, because a
  sequence has no element identity;
* `after` — the body after the test, always of type `T`;
* `null` means that the body did not change. This is the only way to tell the framework that the record
  has no changes: an empty snapshot (`DiffObjectValue` without fields, `DiffCollectionValue` without
  elements) is a value, not an absence of changes, so a snapshot is never `null`;
* `renderUnchanged` is called when the body did not change but the attributes **or the type** of the
  value did; a `null` body never reaches it. By default, it renders a snapshot of the body; a
  renderer that writes only changes (like `Map`, `Set` and `MultiValueMap`) overrides it and returns
  `null`.

`Set` elements are matched by the equality of the set itself (`equals`/`hashCode`, or the
comparator of a `SortedSet`), not by the `addEqualsForType` predicates: a set is expected to hold
values that compare well by their own equality. A set of `BigDecimal`, `ZonedDateTime`, `byte[]`
or another type with non-standard equality needs a custom renderer for the set type.

A minimal renderer of a wrapper body that writes elements without names:

```java
public class TagsDiffRenderer implements DiffRenderer<Tags> {

    @Override
    public DiffValue render(Object before, Tags after, ObjectsDifference difference) {
        if (before != null && difference.isEquals(before, after)) {
            return null;
        }
        return new DiffCollectionValue(difference.entries(after.values(), null).toList());
    }
}
```

Use `DiffObjectValue` for named fields, `DiffCollectionValue` for unnamed elements,
`DiffScalarValue` and `DiffNullValue`. The `ObjectsDifference` methods — `insert`, `update`,
`deleteElement`, `deleteValue`, `entries`, `forEachField` — build elements and keys by the common
rules, so a renderer does not duplicate ordering and identity. `insert` writes a whole value,
`update` only the changes (or `null` when nothing changed), `deleteElement` removes a keyed
element without a body, `deleteValue` removes a value of a repeated field with its body. Get the
elements of a collection or an array through `entries`: an array and an ordered collection keep the
order of the source, a hash based one is sorted by the full element entry.

## Value comparison — `addEqualsForType`

If a change of a type is not a change (for example, `SecureRandom`), register an equality predicate —
such a record will not appear in a diff:

```java
@Bean
TestSetupModule testSetupModule() {
    return new TestSetupModule()
            .addEqualsForType(SecureRandom.class, (first, second) -> true);
}
```

A user predicate is the strongest comparison rule for a pair of values: it is applied before the
renderer and before the type and body-presence checks. When the predicate says "equal", the record
does not appear in the diff, even if the value changed its type or lost its body.

Library presets (`BigDecimal`, `ZonedDateTime`, `Temporal`, `Map`, `SecureRandom` and reference
types) are an equality rule. For plain scalars (`BigDecimal` `0.1` and `0.1000`) a preset suppresses
the diff; for structured values it hides neither attributes nor a type change. A user predicate is
stronger: it can hide both attributes and a type change — that is a deliberate choice.

## Custom data types

For custom Redis structures (your own codec, schema or accessor) it is enough to describe how the
framework reads and writes them — see [Redis Extensibility](redis/Extensibility.md). Common value
serialization rules are in [Serde](serde/README.md).

---
[← Storage Changes](Storage-Diff.md) · [Back to home](README.md)
