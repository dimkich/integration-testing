Kafka Records: Sending and Asserting
====================================

[Russian version](../../ru/kafka/Records-and-Headers.md)

The KafkaRecord model
---------------------

A single Kafka message in tests is described by the `KafkaRecord` type:

| Field        | XML       | Description                                         |
|--------------|-----------|-----------------------------------------------------|
| `connection` | attribute | connection name from the configuration              |
| `topic`      | attribute | topic name                                          |
| `key`        | element   | message key (may be absent)                         |
| `value`      | element   | message value (may be absent — this is a tombstone) |
| `headers`    | element   | headers; repeated tags accumulate into a value list |
| `partition`  | element   | partition number                                    |
| `offset`     | element   | offset in the partition                             |
| `timestamp`  | element   | message time                                        |

The `partition`, `offset` and `timestamp` fields are filled in by the sniffer when reading messages and
are usually excluded from comparison via `excluded-fields` (see [Configuration](Configuration.md)).

Sending messages from the test (`<inboundMessage>`)
--------------------------------------------------

An `<inboundMessage>` block publishes a message to a topic and thereby triggers the case. This is one of
two mutually exclusive ways to start it: if a test has both `<inboundMessage>` and a method call
(`<bean>`/`<method>`), the framework only sends the message and does not call the method. If you need
both actions, split them into separate cases or `Part`s.

```xml

<inboundMessage type="KafkaRecord" connection="kafka1" topic="order-in">
    <key>order-1</key>
    <value>{"orderId": 1, "item": "Book"}</value>
    <partition>0</partition>
    <headers>
        <source>test</source>
        <tag>first</tag>
        <tag>second</tag>
    </headers>
</inboundMessage>
```

How it works:

* the message is serialized by the serializer configured for the topic (strings by default);
* if the message time is not set, the "virtual" time from initialization
  (`<init type="DateTimeInit" .../>`) is used — handy for deterministic assertions;
* sending is synchronous: the message is already in the topic when the case starts;
* the message offset is remembered, so it can be told apart from application messages
  (see `ignore-inbound` in [Configuration](Configuration.md)).

You can send to several topics — just specify several `<inboundMessage>` blocks in a row.

A tombstone (a key without a value) is described by an empty element:

```xml

<inboundMessage type="KafkaRecord" connection="kafka1" topic="order-in">
    <key>order-1</key>
</inboundMessage>
```

Verifying application messages (`<outboundMessage>`)
---------------------------------------------------

The background consumer reads all user topics and picks up everything the application sends.
Expectations are described by `<outboundMessage>` blocks:

```xml

<outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
    <key>order-1</key>
    <value>{"orderId": 1, "status": "PROCESSED"}</value>
</outboundMessage>
```

Rules:

* at the end of the case, captured messages are compared with the expected ones taking
  `excluded-fields` into account; any mismatch is shown as a visual Diff;
* use the main connection to assert application responses — messages sent by the test itself are
  excluded from the results (`ignore-inbound: true` by default);
* if you need the whole stream, add a separate "monitor" connection with `ignoreInbound: false` (see
  [Configuration](Configuration.md));
* several `<outboundMessage>` blocks are checked independently — their number does not have to match
  the number of calls in the case.

Verifying application state (`<dataStorageDiff>`)
-------------------------------------------------

`<outboundMessage>` shows what the application **sent**. If you need to verify what it **received** or
how its state changed, use the regular `<dataStorageDiff>` — for example, over a storage maintained by
your test fixture:

```xml

<dataStorageDiff type="MapStringKeyObjectValue">
    <receivedMessages type="MapStringKeyObjectValue">
        <msg-0 type="KafkaRecord" topic="order-in">
            <key>order-1</key>
            <value>{"orderId": 1, "item": "Book"}</value>
        </msg-0>
    </receivedMessages>
</dataStorageDiff>
```

Here `receivedMessages` is an example storage: in the module tests this is the bean where a listener
stores the messages it received. In your project it can be any storage (a database, a cache, a bean
collection). See [Test Initialization](../Initialization.md) for more about diffs.

Headers
-------

Headers are a multi-value map: the same header may appear several times.

```xml

<headers>
    <tag>first</tag>
    <tag>second</tag>
    <source>test</source>
</headers>
```

In this example the `tag` header gets a list of two values in order of appearance, and `source` gets a
list with a single value.

Notes:

* in XML, repeated tags with the same name accumulate into a list (in JSON, use an array);
* during serialization, headers are written "as is" (plain format) or through the Spring mapper —
  this depends on the `headers` setting (see [Serde](Serde.md));
* Spring formats may add service headers (`__TypeId__`, `spring_json_header_types`) and Kafka
  technical headers (`kafka_offset`, `kafka_receivedTimestamp`, etc.) — they are usually excluded via
  `excluded-fields`.

Tombstone, partitions and time
------------------------------

* **Tombstone** — an empty `<value>` or an empty element entirely: assert it like a regular message,
  leaving `<value>` empty.
* **Partition** — specify `<partition>` in `<inboundMessage>` if the application cares about a specific
  partition. When reading, the field is filled in automatically.
* **Time** — the virtual test time is substituted on send; the real broker time is used on read. To
  keep comparisons stable, `timestamp` is usually added to `excluded-fields`.

Deserialization errors
----------------------

If the sniffer cannot parse an application message, the case does not fail immediately: the record is
stored "raw" (UTF-8) together with the error description, and the exception is rethrown later, during
assertions. In an expectation, such a record is described with an `<exception>` block:

```xml

<outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
    <exception>
        <exceptionType>MismatchedInputException</exceptionType>
        <message>Cannot construct instance of `com.example.OrderEvent` ...</message>
    </exception>
    <key>error-invalid-json</key>
    <value>"{invalid json}"</value>
</outboundMessage>
```

Such a test is useful for verifying application behavior on "bad" data. If this is not an expected
situation, fix the `deserializer` for the topic (see [Serde](Serde.md)) or check the data format.

Complete example
----------------

```xml

<test type="Container">
    <test type="Case" name="Order processed">
        <init type="DateTimeInit" dateTime="2026-05-31T11:30:00Z"/>

        <!-- The framework sends the order -->
        <inboundMessage type="KafkaRecord" connection="kafka1" topic="order-in">
            <key>order-1</key>
            <value>{"orderId": 1, "item": "Book"}</value>
            <headers>
                <source>test</source>
            </headers>
        </inboundMessage>

        <!-- The application processes the order and sends an event -->
        <outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
            <key>order-1</key>
            <value>{"orderId": 1, "status": "PROCESSED"}</value>
        </outboundMessage>
    </test>

    <test type="Case" name="Order rejected">
        <bean>orderService</bean>
        <method>reject</method>
        <request>order-2</request>

        <outboundMessage type="KafkaRecord" connection="kafka1" topic="order-rejected">
            <key>order-2</key>
            <value>{"orderId": 2, "reason": "EMPTY_ITEM"}</value>
        </outboundMessage>
    </test>
</test>
```

---
[← Back to Home](../README.md)
