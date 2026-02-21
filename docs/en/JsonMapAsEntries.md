### `@JsonMapAsEntries` Annotation

The `@JsonMapAsEntries` annotation is a tool for controlling the representation of
collections (`Map`, `Iterable`, `Iterator`) in JSON and XML formats. It allows switching from standard mapping to
entry-list structures, which is essential for working with attributes and complex objects as keys in XML.

*** ** * ** ***

### 1. Configuration Parameters

| Parameter            | Type          | Description                                                                           |
|----------------------|---------------|---------------------------------------------------------------------------------------|
| **`entryFormat`**    | `EntryFormat` | **Required** . Defines the key visualization: `KEY_AS_ATTRIBUTE` or `KEY_AS_ELEMENT`. |
| **`entriesWrapped`** | `boolean`     | If `true`, entries are wrapped in a parent tag (defaults to `<entry>`).               |

*** ** * ** ***

### 2. Entry Formats (`EntryFormat`)

### 2.1. KEY_AS_ATTRIBUTE

The map key is written as an XML attribute named `key`. Recommended for simple data
types (`String`, `Number`, `BigDecimal`).  
**Code Example:**  
java

    @JsonMapAsEntries(entryFormat = EntryFormat.KEY_AS_ATTRIBUTE)
    private Map<String, String> properties;

**Result (XML):**  
xml

    <properties key="color">red</properties>
    <properties key="size">large</properties>

### 2.2. KEY_AS_ELEMENT

The key is written as a nested `<key>` element. Used for complex objects (POJOs).  
**Code Example:**  
java

    @JsonMapAsEntries(entryFormat = EntryFormat.KEY_AS_ELEMENT)
    private Map<Object, Object> data;

**Result (XML):**  
xml

    <data>
        <key type="UserKey"><id>101</id></key>
        <value>Active</value>
    </data>

*** ** * ** ***

### 3. Specialized Presets (LinkedHashMap)

The library provides pre-configured classes that already have `@JsonMapAsEntries` settings. They ensure predictable XML
output and preserve the insertion order of elements.

### 3.1. `LinkedHashMapStringObject<K, V>`

**Configuration:** `KEY_AS_ATTRIBUTE` + `entriesWrapped = true`.  
Designed for maps with simple keys and arbitrary values.  
**Example XML:**  
xml

    <bids>
        <entry key="1.20">10.5</entry>
        <entry key="1.35" type="BigDecimal">12.0</entry>
    </bids>

### 3.2. `LinkedHashMapObjectObject<K, V>`

**Configuration:** `KEY_AS_ELEMENT` + `entriesWrapped = true`.  
Designed for maps where keys are complex objects.  
**Example XML:**  
xml

    <orders>
        <entry>
            <key type="OrderKey">
                <id>123</id>
            </key>
            <value>Confirmed</value>
        </entry>
    </orders>

*** ** * ** ***

### 4. Working with Iterators

When using the annotation on an `Iterator` (or `Iterable`) type, the data is automatically converted into the entries
format. This activates the `ResettableIterator` mechanism:

* The iterator becomes "reusable," automatically resetting its state (`reset`) during each serialization.
* This allows serializing the same iterator object multiple times (e.g., for logging and subsequent use in test
  assertions) without data loss.

*** ** * ** ***

### 5. Scope of Application

1. **FIELD**: Individual map configuration within a DTO.
2. **TYPE**: Global format configuration for all descendants (used in presets).
3. **Inheritance**: An annotation on a base type is automatically inherited by all its implementations registered in the
   library's type system.
