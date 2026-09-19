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

**Whole record.** Configured once for the entire message:

```yaml
"order-out":
  deserializer:
    type: json
    target-class: com.example.kafka.OrderEvent
```

**Per part.** Key, value and headers are configured separately:

```yaml
"order-out":
  deserializer:
    key:
      type: string
    value:
      type: json
      target-class: com.example.kafka.OrderEvent
    headers:
      type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
```

The styles cannot be mixed at one level: `bean-ref` does not combine with parts, `type` does not
combine with `value`, etc. The full table of allowed combinations is in
[Configuration](Configuration.md).

Provider reference
------------------

| `type`                     | Available at      | What it does                                                               |
|----------------------------|-------------------|----------------------------------------------------------------------------|
| `string`                   | record and parts  | UTF-8 strings                                                              |
| `json`                     | record and parts  | Jackson JSON, supports `target-class` and `object-mapper-ref`              |
| `xml`                      | record and parts  | Jackson XML, supports `target-class` and `object-mapper-ref`               |
| `yaml`                     | record and parts  | YAML                                                                       |
| `bytes`                    | record and parts  | raw bytes                                                                  |
| `spring-json`              | whole record only | Spring Kafka `JsonSerializer`/`JsonDeserializer` with type info in headers |
| `spring-xml`               | whole record only | the same for XML                                                           |
| fully qualified class name | record and parts  | the class is created and used as is                                        |
| `bean-ref`                 | record and parts  | a ready Spring bean is used as is                                          |

The `spring-json`/`spring-xml` providers do not work inside `key`/`value`/`headers`: for parts use
`json`/`xml` — same format, but without type information.

If only `value` is configured for a part, the `key` stays a string and `headers` stay plain.

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

* **plain format (default)** — each value is written as a string; repeated values are preserved in
  order of appearance. Suitable for simple text headers.
* **`DefaultKafkaHeaderMapper`** — a Spring mapper that understands types and correctly handles the
  service headers `__TypeId__`, `spring_json_header_types`. It is specified explicitly:

```yaml
headers:
  type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
```

Only context-free serializers are allowed for headers (strings, JSON, XML, bytes, native classes).
Using a whole-record provider there fails at startup with a clear error.

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
