Types and Serde
===============

[Russian version](../../ru/postgresql/Types-and-Serde.md)

The module converts PostgreSQL values into regular Java/XML values, so they can be compared in
`dataStorageDiff`. Built-in converters cover most types. Individual columns can use custom serde.

## Built-in Conversions

| PostgreSQL                                            | Diff / XML representation                 |
|-------------------------------------------------------|-------------------------------------------|
| `text`, `varchar`, `char`                             | `String`                                  |
| `uuid`, `inet`, `xml`, `interval`, ranges, `tsvector` | `String`                                  |
| enums                                                 | `String`                                  |
| `bit`, `varbit`                                       | `String` of `0` and `1`                   |
| `bool`                                                | `Boolean`                                 |
| `int2`, `smallint`, `smallserial`                     | `Short`                                   |
| `int4`, `integer`, `serial`                           | `Integer`                                 |
| `int8`, `bigint`, `bigserial`                         | `Long`                                    |
| `float4`, `real`                                      | `Float`                                   |
| `float8`, `double precision`                          | `Double`                                  |
| `numeric`, `decimal`                                  | `BigDecimal`; `NaN`/`Infinity` as strings |
| `date`                                                | `LocalDate`                               |
| `time`                                                | `LocalTime`                               |
| `timestamp`                                           | `LocalDateTime`                           |
| `timestamptz`                                         | `LocalDateTime` in the test zone          |
| `timetz`                                              | `OffsetTime`                              |
| `json`, `jsonb`                                       | object, array, or scalar                  |
| `hstore`                                              | `Map`                                     |
| arrays                                                | `List`, including nested arrays           |
| composite types                                       | `Map`                                     |
| `bytea`                                               | `byte[]`; Base64 in XML                   |

Domains are resolved to their base type. If a type has no dedicated converter, the value remains a
string.

### Time

`timestamptz` is converted to the zone used by `@MockJavaTime`, or to UTC when time is not mocked. In
XML the value has type `LocalDateTime`:

```xml
<ts_tz type="LocalDateTime">2026-01-01T10:15:30.123456</ts_tz>
```

`timetz` preserves the offset and has type `OffsetTime`. In DBUnit fixtures, prefer an explicit offset
for time values.

### JSON and JSONB

`json`/`jsonb` are parsed by Jackson. An object becomes a map, an array becomes a list, and a scalar
stays a regular value. Floating-point numbers are parsed as `BigDecimal`.

Use `target-class` when a specific DTO is needed:

```yaml
columns:
  "[public.events.payload]":
    type: json
    target-class: com.example.EventPayload
```

### Arrays

Arrays are represented as `ArrayList`. Multidimensional arrays, `NULL` elements, quotes, and escapes
are supported. The lower bound is not shown in the diff; element order matters.

```xml
<tags type="ArrayList">
    <tags>tag_a</tags>
    <tags>tag_b</tags>
    <tags xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
</tags>
```

### Composite Types

A composite type becomes a map keyed by field names. `NULL` fields appear as `nil`:

```xml
<addr type="MapStringKeyObjectValue">
    <city xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    <zip>220000</zip>
</addr>
```

### BYTEA

`bytea` is decoded to `byte[]` and shown as Base64 in XML:

```xml
<bin type="byte[]">BAUG</bin>
```

An empty byte array is an empty tag:

```xml
<bin type="byte[]"></bin>
```

The module expects the standard `bytea_output = 'hex'`. With the escape format, the value may remain
text.

## Custom Column Converters

Serde settings are defined in the connection's `columns` section. The key is `table.column` or
`schema.table.column`:

```yaml
integration:
  testing:
    postgresql:
      connections:
        dataSource:
          columns:
            "[public.spi_values.location]":
              type: it-point
            "[public.serde_values.data]":
              type: json
              target-class: com.example.UserData
            "[public.serde_values.payload]":
              type: string
              binary-envelope: "{VER(1)}{LEN(INT, BE)}{CONTENT}"
```

Source options:

* `type` without a dot — a registered provider name, for example `it-point`;
* `type` with a dot — a fully qualified class name;
* `bean-ref` — a ready Spring bean name.

`target-class` and `object-mapper-ref` work with Jackson providers (`json`, `xml`, `yaml`).
`binary-envelope` wraps the selected source; see [Binary Envelopes](../serde/Binary-Envelopes.md).

For `bytea`, PostgreSQL decoding runs first and then your converter is called. A custom `bytea`
converter therefore receives binary data, not the `\x...` text.

### Custom Provider

To add a new format, register a `TestSerdeProviderFactory` as a Spring bean. For PostgreSQL the
context is `PostgresColumnSerdeContext` and the role is `PostgresTypeDescriptor`:

```java
@Component
@Getter
public class PointSerdeProvider implements TestSerdeProviderFactory<ByteBuffer, Object,
        PostgresColumnSerdeContext, PostgresTypeDescriptor, StandardSerdeProperties> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<PostgresColumnSerdeContext> contextClass = PostgresColumnSerdeContext.class;
    private final Class<PostgresTypeDescriptor> roleClass = PostgresTypeDescriptor.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "it-point";

    @Override
    public TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> create(
            StandardSerdeProperties config, Class<ByteBuffer> inputClass, Class<Object> outputClass,
            Class<PostgresColumnSerdeContext> contextClass, PostgresTypeDescriptor role) {
        return TestSerdeConverter.of(ByteBuffer.class, Object.class, PostgresColumnSerdeContext.class,
                (input, context) -> StandardCharsets.UTF_8.decode(input.duplicate()).toString());
    }
}
```

If the role is not needed, use `ComponentRole` as in the general
[Serde extensibility](../serde/Extensibility.md) examples. `PostgresColumnSerdeContext` exposes the table
name, column name, SQL type, and time zone.

### Converter Priority

If a column has a custom converter, it is authoritative: there is no fallback to the built-in
converter. If the custom converter returns `null`, the value is treated as `null`.

A conversion failure reports the table, column, and SQL type:

```text
Failed to convert PostgreSQL column public.events.payload of type jsonb
```

Check the `columns` entry for that column and make sure `target-class` matches the actual JSON.

## Exclusions and Serde

`ignore: true` excludes a column from the diff before conversion. `excludedFields` is applied after
conversion and removes only the listed nested fields. Audit and revert still use the complete value.

---
[← Back to documentation](../README.md)
