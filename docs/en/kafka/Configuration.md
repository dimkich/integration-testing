Kafka Module Configuration: Parameter Reference
===============================================

[Russian version](../../ru/kafka/Configuration.md)

All Kafka settings live in `application-test.yml` under the `integration.testing.kafka` prefix. The
settings have three levels and are inherited from top to bottom: global → connection → topic.

Configuration levels
--------------------

```text
integration.testing.kafka.*                          (global values)
        │
        ▼
integration.testing.kafka.connections.<name>.*        (connection settings)
        │
        ▼
...connections.<name>.topics.<regex>.*                (per-topic settings)
```

Every key in `connections` is a logical connection. The framework creates its own producer, sniffer
consumer and admin client for it. The application keeps using its own clients (for example, configured
through `spring.kafka.bootstrap-servers`).

### Inheritance rules

* scalar values (`ignore`, `ignore-inbound`) are overridden by the child level;
* sets (`excluded-fields`) are merged;
* `serializer` and `deserializer` are merged recursively, but the
  `type`/`bean-ref`/`key`/`value`/`headers` group is exclusive: if the child level sets at least one
  member of the group, the other members are not inherited from the parent. This prevents mixing
  "whole-record setup" from one level with "per-part setup" from another;
* the `connections` and `topics` maps are not inherited — their keys are defined at their own level.

Complete parameter reference
----------------------------

### Global level (`integration.testing.kafka.*`)

| Parameter                               | Type            | Default | Description                                                    |
|-----------------------------------------|-----------------|---------|----------------------------------------------------------------|
| `serializer`                            | Serde object    | not set | default serialization of messages the test sends               |
| `deserializer`                          | Serde object    | not set | default deserialization of captured messages                   |
| `ignore`                                | boolean         | `false` | exclude topics from sniffer capture                            |
| `ignore-inbound`                        | boolean         | `true`  | hide messages sent by the test itself from assertions          |
| `excluded-fields`                       | list of strings | not set | fields excluded from message comparison                        |
| `lag-polling-interval-ms`               | long            | `5`     | pause between lag checks, ms                                   |
| `lag-polling-timeout-ms`                | long            | `10000` | total time to wait for lag to clear, ms                        |
| `readiness-polling-interval-ms`         | long            | `100`   | pause between readiness checks of expected consumer groups, ms |
| `startup-stabilization-timeout-seconds` | long            | `30`    | time to wait for consumer group stabilization at startup, s    |
| `connections`                           | map             | not set | broker connections                                             |

### Connection level (`integration.testing.kafka.connections.<NAME>.*`)

| Parameter                                                                   | Type            | Default  | Description                                                                 |
|-----------------------------------------------------------------------------|-----------------|----------|-----------------------------------------------------------------------------|
| `bootstrap-servers`                                                         | string          | required | broker address: `host:port` or a comma-separated list                       |
| `properties`                                                                | map             | `{}`     | extra Kafka client properties (`acks`, `security.protocol`, `sasl.*`, etc.) |
| `topics`                                                                    | map             | not set  | topic settings; the key is a regular expression                             |
| `expected-groups`                                                           | list of strings | not set  | SUT consumer groups awaited before a test and during the wait cycle         |
| `serializer`, `deserializer`, `ignore`, `ignore-inbound`, `excluded-fields` |                 |          | inherited from the global level                                             |

If `bootstrap-servers` is missing, context startup fails — a connection without a broker address cannot
be created.

### Topic level (`integration.testing.kafka.connections.<NAME>.topics.<REGEX>.*`)

| Parameter         | Type            | Default   | Description                                       |
|-------------------|-----------------|-----------|---------------------------------------------------|
| `serializer`      | Serde object    | inherited | how to serialize messages from `<inboundMessage>` |
| `deserializer`    | Serde object    | inherited | how to deserialize captured messages              |
| `ignore`          | boolean         | inherited | exclude the topic from sniffer capture            |
| `ignore-inbound`  | boolean         | inherited | hide messages sent by the test itself             |
| `excluded-fields` | list of strings | inherited | fields excluded from comparison                   |

### Serde object (`serializer` and `deserializer`)

| Field                       | Description                                                                                                                                      |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`                      | format name (`json`, `xml`, `string`, `bytes`, `yaml`, `spring-json`, `spring-xml`) or a fully qualified class name (`com.example.MySerializer`) |
| `bean-ref`                  | name of a ready Spring bean. The bean is used as is, other fields are ignored                                                                    |
| `target-class`              | class to deserialize the value into. Only works together with a format name                                                                      |
| `object-mapper-ref`         | name of an `ObjectMapper` bean for Jackson formats. Only works together with a format name                                                       |
| `key` / `value` / `headers` | separate Serde settings for key, value and headers                                                                                               |
| `add-type-info-headers`     | `spring-json`/`spring-xml`: add the type to headers during serialization                                                                         |
| `use-type-info-headers`     | `spring-json`/`spring-xml`: read the type from headers during deserialization                                                                    |
| `trusted-packages`          | `spring-json`/`spring-xml`: packages allowed for type resolution (`*` — all)                                                                     |

The `spring-json` and `spring-xml` providers are available only in the whole-record
`serializer`/`deserializer` — inside `key`/`value`/`headers` use `json`/`xml` (same format, but without
type information).

### Allowed and forbidden combinations

| Combination                                                                   | Result                                                                  |
|-------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `type: json`                                                                  | allowed                                                                 |
| `type: json` + `target-class`                                                 | allowed                                                                 |
| `type: json` + `key`/`headers`                                                | allowed: the provider assembles the record, the parts override defaults |
| `type: <fully qualified class name>`                                          | allowed                                                                 |
| `bean-ref: myBean`                                                            | allowed                                                                 |
| `type: json` + `value`                                                        | forbidden: the record provider already defines value serialization      |
| `bean-ref` + `key`/`value`/`headers`                                          | forbidden: the ready bean is used as a whole                            |
| `bean-ref` + `type`                                                           | forbidden: these are mutually exclusive ways to select the source       |
| `type: <fully qualified class name>` + `key`/`headers`                        | forbidden: the class handles the whole record                           |
| `bean-ref` or fully qualified class name + `target-class`/`object-mapper-ref` | forbidden: these fields are not applied to them                         |

An invalid combination fails at context startup, for example:

```text
Invalid serializer config at connection[kafka1].topics[order-in]: Conflicting serde config: ...
```

The message explains what conflicts and how to fix the configuration.

Topic matching by regular expression
------------------------------------

`topics` keys are regular expressions, not exact names. The topic name must fully match the expression.
Rules:

* the first matching expression wins — the order in the file matters;
* the match result is cached on first access to the topic;
* avoid overlapping expressions: otherwise the winner depends on YAML order;
* if nothing matches, the connection settings apply.

```yaml
topics:
  "order-created": # exact name
    serializer:
      type: json
  "order-.*": # anything starting with order-
    deserializer:
      type: json
  ".*-events": # anything ending with -events
    deserializer:
      type: string
```

The `ignore` and `ignore-inbound` flags
---------------------------------------

`ignore: true` — the topic is not captured by the sniffer: application messages from it will not appear
in `<outboundMessage>`. Sending messages to such a topic from the test still works — `<inboundMessage>`
is unaffected.

`ignore-inbound: true` (default) — captured messages that the test itself sent are excluded. This
protects from "echo": if the application forwards or logs incoming messages, they will not show up in
assertions. Set it to `false` for a "monitor" connection when you want the full stream, including what
the test sent:

```yaml
connections:
  kafka1:
    bootstrapServers: ${embedded.kafka.brokerList}
    ignoreInbound: false
    excludedFields:
      - partition
      - offset
      - timestamp
```

Excluding fields (`excluded-fields`)
------------------------------------

A list of paths that are not compared. Nested paths use dots:

```yaml
excluded-fields:
  - partition
  - offset
  - timestamp
  - headers.id
  - headers.kafka_offset
```

Technical fields are excluded most often: `partition`, `offset`, `timestamp` and Kafka service
headers — they always differ and are not part of the test logic.

Waiting for SUT readiness (`expected-groups`)
---------------------------------------------

When application containers start asynchronously (for example, after the Spring context is up), the
broker may not have any SUT consumer groups at the moment of the first test. List them in
`expected-groups` so the framework waits for the groups to appear, stabilize and get partitions
assigned:

```yaml
connections:
  kafka1:
    bootstrapServers: ${embedded.kafka.brokerList}
    expected-groups:
      - fix-adapter.input.id
      - fix-adapter.output.id
```

How it works:

* before every test (`start()`) the framework waits up to `startup-stabilization-timeout-seconds`
  for each group to become ready — live members holding assigned partitions ("running + assigned").
  On timeout the test fails with `Kafka startup stabilization timeout ...`;
* every poll iteration after a message is sent (`waitCompletion()`) repeats the check: while the
  expected groups are not ready, "no lag" is not treated as completion. Readiness checks run no more
  often than once per `readiness-polling-interval-ms`;
* an empty or missing list keeps the previous behavior: only known groups are awaited, and an empty
  listing does not block;
* several connections to the same cluster join their lists.

How readiness is determined depends on the mode:

* `inflight = true` (default) — from the instrumented clients in memory: a group is ready when all of
  its consumers have been assigned partitions (rebalance listener callbacks); with a manual `assign`
  it is ready right after the call. The broker is not contacted; groups of external processes are not
  visible — use `inflight = false` for them;
* `inflight = false` — through the Admin API: the group must be `STABLE` with live members holding
  assigned partitions.

Limitation of the `inflight = false` mode: groups that assign partitions manually
(`@KafkaListener(topicPartitions = ...)`) appear as `EMPTY` on the broker (they have no group
members), so they must not be listed in `expected-groups` — waiting for them ends with an error. In
`inflight = true` manual `assign` is tracked, and such groups may be listed.

Real broker in tests
--------------------

`@EnableTestKafka` adds `real-kafka.properties`: both the application and the framework talk to a real
broker started by Testcontainers in Docker. The broker address is available as
`${embedded.kafka.brokerList}`. Kafka tests will not run when Docker is not running.

Complete configuration example
------------------------------

```yaml
integration:
  testing:
    kafka:
      connections:
        kafka1:
          bootstrapServers: "${embedded.kafka.brokerList}"
          expected-groups:
            - order-processor
          excluded-fields:
            - partition
            - offset
            - timestamp
            - headers.id
            - headers.kafka_offset
          topics:
            "order-in":
              serializer:
                type: json
            "order-out":
              deserializer:
                type: json
                target-class: com.example.kafka.OrderEvent
            "notification-.*":
              serializer:
                type: spring-json
                add-type-info-headers: true
              deserializer:
                type: spring-json
                use-type-info-headers: true
                trusted-packages: "*"
        monitor:
          bootstrapServers: "${embedded.kafka.brokerList}"
          ignoreInbound: false
          excluded-fields:
            - partition
            - offset
            - timestamp
```

---
[← Back to Home](../README.md)
