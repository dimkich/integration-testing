Serialization and Deserialization (Serde)
=========================================

[Russian version](../../ru/serde/README.md)

Serde defines how your data goes out and comes in: JSON, XML, a string, raw bytes or a binary
envelope. Format settings live in `application-test.yml` — for a whole message or value, or for its
individual parts (in Kafka — `key`/`value`, in Redis — `value`/`hash-key`/`hash-value`). This page
is the shared part; platform specifics live in the [Kafka](../kafka/Serde.md) and
[Redis](../redis/Configuration.md) sections.

Where the format comes from
---------------------------

The format source is set in one of three ways:

* `type` **without a dot** — the name of a built-in format: `json`, `xml`, `yaml`, `string`, `bytes`;
* `bean-ref` — the name of a ready-made Spring bean (for example, your own serializer);
* `type` **with a dot** — a fully qualified class name: Spring creates the bean itself.

If your project already has native serializers (Kafka `Serializer`/`Deserializer`, Spring Data
`RedisSerializer`, Redisson `Codec`), they can also be referenced via `bean-ref` or by class — the
framework will wire them in.

If you need a custom format — see [Extensibility](Extensibility.md).

Source selection
----------------

| Field      | What it does                                                                             |
|------------|------------------------------------------------------------------------------------------|
| `type`     | provider name (`json`, `xml`, `string`, `yaml`, `bytes`) or a fully qualified class name |
| `bean-ref` | name of a ready-made Spring bean: the source is taken as is, except `binary-envelope`    |

* `type` **without a dot** — the name of a registered provider. The set of available names depends on
  the config level and the platform.
* `type` **with a dot** — a fully qualified class name: Spring creates the bean, and `target-class` /
  `object-mapper-ref` are not applied to it.
* `bean-ref` and `type` are mutually exclusive at one level: they are two ways to select a source.

Common fields
-------------

| Field               | Applies with       | Description                                                                |
|---------------------|--------------------|----------------------------------------------------------------------------|
| `target-class`      | provider name only | the class serialization works with and the class to deserialize into       |
| `object-mapper-ref` | provider name only | name of an `ObjectMapper` bean for Jackson formats (`json`, `xml`, `yaml`) |
| `binary-envelope`   | next to the source | binary envelope template — see [Binary Envelopes](Binary-Envelopes.md)     |

`target-class` and `object-mapper-ref` only work together with a provider name: `bean-ref` returns a
ready-made bean as is, and a class referenced by FQCN is created by Spring without extra
configuration. `binary-envelope` is the exception: the envelope is applied to `bean-ref`, FQCN and
provider sources alike — see [Binary Envelopes](Binary-Envelopes.md).

`binary-envelope` without a source at the same level is rejected: there is nothing to wrap. In Redis
a schema component inherits its source from the parent schema by the property merge, so a component
may set only `binary-envelope` — see
[Redis Configuration](../redis/Configuration.md#schema-component-inheritance).

Core providers
--------------

| `type`   | Format        | `target-class`/`object-mapper-ref` |
|----------|---------------|------------------------------------|
| `string` | UTF-8 strings | no                                 |
| `json`   | Jackson JSON  | yes                                |
| `xml`    | Jackson XML   | yes                                |
| `yaml`   | Jackson YAML  | yes                                |
| `bytes`  | raw bytes     | no                                 |

Which providers are available at a particular level (whole record, record part, schema component)
and platform formats (`spring-json`, `spring-xml`) are described on the platform pages:
[Kafka](../kafka/Serde.md), [Redis](../redis/Configuration.md).

Settings inheritance
--------------------

Serde settings are merged across levels (global → connection → topic/schema):

* simple fields are overridden by the child level;
* nested settings are merged recursively;
* if a child level sets its own source (`type` or `bean-ref`) or splits the setting into parts
  (`key`/`value`/`headers` or `value`/`hash-key`/`hash-value`), the parent setting is not mixed in:
  you cannot inherit a "whole source" and override a part of it at the same time;
* missing parts can be filled from the parent platform setting: in Redis a partially configured
  schema (for example, only `value`) takes the missing codecs from the parent schema — see
  [Redis Configuration](../redis/Configuration.md#schema-component-inheritance).

If incompatible fields end up at the same level (for example, `bean-ref` together with `type`), the
context fails to start with a descriptive error — see "Diagnostics".

Diagnostics
-----------

| Error                                                                                                                  | Cause and fix                                                                                                                                  |
|------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `Unknown serde provider [x] for config [C]. Known providers: [...]`                                                    | There is no provider with this name for this level. Check the available names in the error text and on the platform page.                      |
| `No converter for [X -> Y] under config [C]`                                                                           | No converter factory matches the config class (no source, no factory for this config). Usually it means the config is used at the wrong level. |
| `No adapter from [X] to [Y] under config [C]`                                                                          | This source type is not supported. Use a supported format; for a custom one — see [Extensibility](Extensibility.md).                           |
| `Both 'beanRef' and 'type' are set on the same serde config`                                                           | Mutually exclusive ways to select a source. Keep one of them.                                                                                  |
| `No bean named [x] ...` / `Bean named [x] is not of type [...]`                                                        | The bean from `bean-ref`/`object-mapper-ref` is not found or has a different type. Check the name and define a bean of the required type.      |
| `Field 'targetClass'/'objectMapperRef' has no effect when 'beanRef' is set` (or `... is a fully qualified class name`) | These fields only work with a provider name. Remove them or switch to `type: <provider>`.                                                      |
| `Field 'binaryEnvelope' has no effect without a source ...`                                                            | The envelope wraps a source, and there is no source at this level. Put `binary-envelope` next to the source.                                   |

If you need a custom format, factory or adapter — see [Extensibility](Extensibility.md).

---
[← Back to Home](../README.md)
