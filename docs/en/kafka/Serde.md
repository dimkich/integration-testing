Serialization and Deserialization (Serde)
=========================================

[Russian version](../../ru/kafka/Serde.md)

Why it is needed
----------------

The framework exchanges raw bytes with the broker: an `<inboundMessage>` must be serialized before it is
sent, and captured application messages must be deserialized to be shown in the Diff. The
`serializer`/`deserializer` topic settings control how this is done. The application's own clients are
not replaced — the application keeps using its own serializer.

Default setup
-------------

If nothing is configured for a topic, strings are used:

* `key` — string (UTF-8);
* `value` — string (UTF-8);
* `headers` — plain headers "as is".

This is enough for a quick start and text messages.

Two configuration styles
------------------------

**Record base.** The record-level fields `type`, `bean-ref`, `target-class`, `object-mapper-ref`
and the `spring-json`/`spring-xml` settings are a base for all parts: when the configuration is
loaded they are merged into the `value`, `key` and `headers` components, and a value configured
in a component overrides the base. When the base does not define a format, the parts use the
defaults: `key` and `value` are strings, `headers` are plain.

```yaml
"order-out":
  deserializer:
    type: json
    target-class: com.example.kafka.OrderEvent
    key:
      type: string
```

**Per part.** Key, value and headers are configured separately; parts override the base field by
field:

```yaml
"order-out":
  deserializer:
    type: json
    target-class: com.example.kafka.OrderEvent
    key:
      type: string
    headers:
      type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
```

Components may also be configured explicitly — the base fills their undefined fields:
for example, `value: {binary-envelope: ...}` works together with the record-level `type: json`.

Provider reference
------------------

The providers shared by all platforms (`string`, `json`, `xml`, `yaml`, `bytes`), the
`target-class`/`object-mapper-ref` fields and the source selection rules are described in the
[Serde overview](../serde/README.md). In Kafka the core formats are available both as a whole
record and as parts; platform providers are whole-record only:

| `type`                                   | Available at     | What it does                                                                                                        |
|------------------------------------------|------------------|---------------------------------------------------------------------------------------------------------------------|
| `string`, `json`, `xml`, `yaml`, `bytes` | record and parts | core formats — see the [Serde overview](../serde/README.md)                                                         |
| `spring-json`                            | record and parts | Spring Kafka `JsonSerializer`/`JsonDeserializer` with type info in headers; in `headers` — the Spring header mapper |
| `spring-xml`                             | record and parts | the same for XML                                                                                                    |
| fully qualified class name               | record and parts | the class is created and used as is                                                                                 |
| `bean-ref`                               | record and parts | a ready Spring bean is used as is                                                                                   |

At the record level, `type`/`bean-ref` may also point to a native Kafka `Serializer`/`Deserializer`:
after the merge it becomes the serde of every part, unless the part is overridden by its own
configuration.

`spring-json`/`spring-xml` can be applied to individual parts as well: in `key`/`value` they work as
`JsonSerializer`/`JsonDeserializer` (`__KeyTypeId__` for the key, `__TypeId__` for the value), and in
`headers` as the Spring header mapper.

If the base is not set and only `value` is configured, the `key` stays a string and `headers` stay
plain.

`binary-envelope` — a binary envelope template (see [Binary Envelopes](../serde/Binary-Envelopes.md)) —
can be added only to the `key`/`value` parts: a core provider (`json`/`xml`/…) or a native Kafka
`Serializer`/`Deserializer` (FQCN `type` or `bean-ref`). A whole-record `binary-envelope` is not
supported: to wrap the value, configure the envelope on `value` (for example,
`value: {binary-envelope: ...}` together with the record-level `type: json`). `binary-envelope`
is not supported for `headers`: headers are serialized per value.

The `spring-json` and `spring-xml` settings
-------------------------------------------

These providers write the class name into Kafka headers, which allows deserializing polymorphic data:

| Field                   | Applies to      | Description                                                               |
|-------------------------|-----------------|---------------------------------------------------------------------------|
| `add-type-info-headers` | serialization   | add the class name to headers (`true`/`false`); Spring adds it by default |
| `use-type-info-headers` | deserialization | read the class name from headers                                          |
| `trusted-packages`      | deserialization | packages allowed for type resolution; `*` allows all                      |
| `target-class`          | deserialization | default class when there is no type in headers                            |

Example of a polymorphic pair:

```yaml
"order-events":
  serializer:
    type: spring-json
    add-type-info-headers: true
  deserializer:
    type: spring-json
    use-type-info-headers: true
    trusted-packages: "*"
```

When writing a message in XML, you can specify the value type explicitly, and the framework will
serialize it as an object:

```xml

<outboundMessage type="KafkaRecord" connection="kafka1" topic="order-events">
    <key>order-1</key>
    <value type="OrderEventDto">
        <orderId>order-1</orderId>
        <status>PROCESSED</status>
    </value>
    <headers>
        <__TypeId__>com.example.kafka.OrderEventDto</__TypeId__>
    </headers>
</outboundMessage>
```

Header serialization
--------------------

Headers can be described in two ways:

* **plain format (the default when the base does not define a header format)** — each value is
  written as a string; repeated values are preserved in order of appearance. Suitable for simple
  text headers.
* **`DefaultKafkaHeaderMapper`** — a Spring mapper that understands types and correctly handles the
  service headers `__TypeId__`, `spring_json_header_types`. It is specified explicitly:

```yaml
headers:
  type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
```

Only context-free serializers are allowed for headers (strings, JSON, XML, bytes, native classes).
`spring-json`/`spring-xml` in headers produce the Spring header mapper; `binary-envelope` is not
supported for headers.

Configuration examples
----------------------

```yaml
topics:
  # Default strings — no configuration needed
  "text-in": { }

  # Whole record via JSON
  "json-in":
    serializer:
      type: json
  "json-out":
    deserializer:
      type: json
      target-class: com.example.kafka.JsonDto

  # Per part: string key, JSON value, Spring mapper for headers
  "json-parts-in":
    serializer:
      key:
        type: string
      value:
        type: json
      headers:
        type: org.springframework.kafka.support.DefaultKafkaHeaderMapper

  # Bytes
  "byte-in":
    serializer:
      type: bytes

  # Native Kafka classes
  "text-native-in":
    serializer:
      key:
        type: org.apache.kafka.common.serialization.StringSerializer
      value:
        type: org.apache.kafka.common.serialization.StringSerializer

  # A ready serializer bean
  "custom-in":
    serializer:
      bean-ref: myCustomRecordSerializer
```

If you need a custom format
---------------------------

If no provider fits, you can write your own serializer, deserializer or format provider — see
[Extensibility](Extensibility.md).

---
[← Back to Home](../README.md)
