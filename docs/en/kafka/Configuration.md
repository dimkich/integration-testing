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
* `serializer` and `deserializer` are merged recursively: `type`/`bean-ref` from the parent and the
  child levels do not mix (two mutually exclusive ways to select the source), while `key`/`value`/
  `headers` are inherited and merged field by field;
* the `connections` and `topics` maps are not inherited — their keys are defined at their own level.

Complete parameter reference
----------------------------

### Global level (`integration.testing.kafka.*`)

| Parameter                               | Type            | Default | Description                                                             |
|-----------------------------------------|-----------------|---------|-------------------------------------------------------------------------|
| `serializer`                            | Serde object    | not set | default serialization of messages the test sends                        |
| `deserializer`                          | Serde object    | not set | default deserialization of captured messages                            |
| `ignore`                                | boolean         | `false` | exclude topics from sniffer capture                                     |
| `ignore-inbound`                        | boolean         | `true`  | hide messages sent by the test itself from assertions                   |
| `excluded-fields`                       | list of strings | not set | fields excluded from message comparison                                 |
| `lag-polling-interval-ms`               | long            | `5`     | pause between lag checks, ms                                            |
| `lag-polling-timeout-ms`                | long            | `10000` | total time to wait for lag to clear, ms                                 |
| `readiness-polling-interval-ms`         | long            | `100`   | pause between readiness checks of expected consumer groups, ms          |
| `startup-stabilization-timeout-seconds` | long            | `30`    | time to wait for SUT readiness (sniffer + groups) before lag polling, s |
| `connections`                           | map             | not set | broker connections                                                      |

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

The common fields (`type`, `bean-ref`, `target-class`, `object-mapper-ref`) and the
source selection rules are described in the [Serde overview](../serde/README.md). The
`binary-envelope` field is available only inside `key`/`value`. This section lists
Kafka specifics only:

| Field                       | Description                                                                   |
|-----------------------------|-------------------------------------------------------------------------------|
| `key` / `value` / `headers` | separate Serde settings for key, value and headers                            |
| `add-type-info-headers`     | `spring-json`/`spring-xml`: add the type to headers during serialization      |
| `use-type-info-headers`     | `spring-json`/`spring-xml`: read the type from headers during deserialization |
| `trusted-packages`          | `spring-json`/`spring-xml`: packages allowed for type resolution (`*` — all)  |

The `spring-json` and `spring-xml` providers can be applied to individual parts as well: in
`key`/`value` they work as Spring `JsonSerializer`/`JsonDeserializer` (`__KeyTypeId__` for the key),
and in `headers` as the Spring header mapper (`DefaultKafkaHeaderMapper`).

### Allowed and forbidden combinations

| Combination                                                                   | Result                                                                                   |
|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| record-level `type`/`bean-ref`/`target-class`/`object-mapper-ref`             | base for all parts: merged into `key`/`value`/`headers`, a part overrides field by field |
| `type: json` + `key`/`value`/`headers`                                        | allowed: the base is applied to every part, the configured parts override it             |
| `key`/`value`/`headers` without a base                                        | allowed: the record is assembled from the components only                                |
| `type` (provider) + `value`                                                   | allowed: the base fills the undefined part fields (e.g. `binary-envelope`)               |
| `bean-ref` + `type` at one level                                              | forbidden: mutually exclusive ways to select the source                                  |
| `bean-ref` or fully qualified class name + `target-class`/`object-mapper-ref` | forbidden: these fields are not applied to them                                          |
| `binary-envelope` in the whole-record `serializer`/`deserializer`             | forbidden: the envelope is applied to parts only (`key`/`value`)                         |

An invalid combination fails at context startup, for example:

```text
Invalid serializer config at connection[kafka1].topics[order-in]: Both 'beanRef' and 'type' are set on the same serde config. ...
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

* `start()` only resets the tracking state and never blocks; readiness is awaited when the wait
  completion runs after test initialization, so SUT listeners started by initialization have time to
  come up;
* every poll iteration (`waitCompletion()`) first waits up to `startup-stabilization-timeout-seconds`
  for the sniffer assignment and for each expected group to become ready — live members holding
  assigned partitions ("running + assigned") — and, when groups are listed, for all known groups to
  stop rebalancing. On timeout the test fails with
  `Kafka wait completion timeout. expected consumer groups are not ready: ...`;
* only then the lag is awaited, up to `lag-polling-timeout-ms`. Readiness checks run no more often than
  once per `readiness-polling-interval-ms`;
* an empty or missing list does not block: no expected group is required, so group readiness is not
  awaited at all — only the sniffer assignment and the lag are;
* several connections to the same cluster join their lists.

How readiness is determined depends on the mode:

* `inflight = true` (default) — from the application's client state: a group is ready when all of its
  consumers have been assigned partitions; with a manual `assign` it is ready right after the call.
  The broker is not contacted; groups of external processes are not visible — use `inflight = false`
  for them;
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
