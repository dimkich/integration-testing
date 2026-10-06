Troubleshooting Common Kafka Test Issues
========================================

[Russian version](../../ru/kafka/Troubleshooting.md)

Error: `Kafka wait completion timeout. Unprocessed messages remaining on broker.`
--------------------------------------------------------------------------------

### Symptoms

A case fails with `Kafka wait completion timeout. Unprocessed messages remaining on broker.` The error
usually appears when the application does not have enough time to process messages.

### Cause 1: the application needs more time

The application processes messages in batches, calls an external service or replies with a delay.

**Fix:** increase `lag-polling-timeout-ms` (10000 ms by default):

```yaml
integration:
  testing:
    kafka:
      lag-polling-timeout-ms: 20000
```

### Cause 2: the consumer does not commit offsets

The application reads messages but does not acknowledge them (for example, auto-commit is disabled and
manual commit is not configured), or it deliberately skips a failed record without committing its
offset. The framework then still considers the messages unprocessed.

**Fix:** make the application commit the offset of the skipped record (for example,
`DefaultErrorHandler.setCommitRecovered(true)`) or force the commit from the test with a method hook.
Both options are described in [Error Scenarios and Uncommitted Offsets](Error-Handling.md).

### Cause 3: a message remained in an open transaction

The application sent a message inside a transaction but did not commit it. In the default mode such
messages are intentionally not counted as processed.

**Fix:** check the transaction logic in the application; for diagnostics, temporarily use
`@EnableTestKafka(inflight = false)` — the Admin API mode will show what the broker sees.

### Cause 4: the SUT has not started yet

The application starts its Kafka containers asynchronously, and at check time the SUT group does not
exist on the broker yet (or is rebalancing). When the groups are listed in
[`expected-groups`](Configuration.md#waiting-for-sut-readiness-expected-groups), the framework waits
for them up to `startup-stabilization-timeout-seconds` while waiting for completion after test
initialization, and then fails with
`Kafka wait completion timeout. expected consumer groups are not ready: ...`.

**Fix:** verify that the SUT containers actually start and increase
`startup-stabilization-timeout-seconds` if needed. Make sure `expected-groups` lists only
subscription-based groups: groups with manual `assign` appear as `EMPTY` on the broker, and waiting
for them will not complete.

Serde resolution errors
-----------------------

`No adapter from [...]`, `Unknown serde provider [...]`,
`No converter for [...]`, `Field 'binaryEnvelope' ...`, config conflicts (`Both 'beanRef' and 'type' are set ...`) and
other serde errors are shared by all platforms; their causes and fixes are collected in the
[Serde overview](../serde/README.md#diagnostics).

In Kafka the error text additionally includes the path to the setting:

```text
Invalid serializer config at connection[kafka1].topics[order-in]: Both 'beanRef' and 'type' are set on the same serde config. ...
```

The full table of allowed combinations is in [Configuration](Configuration.md). If the `serializer`
references a class that cannot serialize messages, use a supported format or a custom record
serializer (see [Extensibility](Extensibility.md)).

Startup error: `Connection [...] must have bootstrapServers configured`
-----------------------------------------------------------------------

### Symptoms

The test context fails to start:

```text
Connection [kafka1] must have bootstrapServers configured
```

### Cause

The connection has no `bootstrap-servers`. The address is required — the framework cannot create
clients without it.

### Fix

Add the broker address to the connection configuration:

```yaml
integration:
  testing:
    kafka:
      connections:
        kafka1:
          bootstrapServers: ${embedded.kafka.brokerList}
```

Error: `Kafka context is being built but no instrumentation plugins are active`
-------------------------------------------------------------------------------

### Symptoms

At test startup the context fails with a message mentioning `@Order on JunitExtension` and
`@IntegrationTesting`.

### Cause

The Kafka context is built before the framework's JUnit extension has activated its plugins. Usually
this means the test was started without `@EnableTestKafka` (or with incompatible extension
ordering).

### Fix

Run the test through `@EnableTestKafka`; do not override the JUnit extension order manually. If the
application's clients are not available, use `@EnableTestKafka(inflight = false)` — a slower mode
that tracks lag through the Admin API.

Method is not called, although it is specified in the test
---------------------------------------------------------

### Symptoms

A case has both `<inboundMessage>` and `<bean>`/`<method>`. The test passes, but the application method
is not executed: for example, the expected `<outboundMessage>` or state change does not appear.

### Cause

A case is triggered either by a message or by a method call — these are mutually exclusive ways. If the
test has an `<inboundMessage>`, the framework only sends the message and ignores `<bean>`/`<method>`.

### Fix

Split the actions into separate cases or `Part`s: one test with `<inboundMessage>` for the Kafka message
and another test with `<bean>`/`<method>` for the method call. See
[Kafka Records](Records-and-Headers.md) for details.

A message does not show up in assertions
----------------------------------------

### Symptoms

`<outboundMessage>` does not find the expected message although the application sent it.

### Cause 1: the topic is excluded from capture

The topic or connection has `ignore: true`.

**Fix:** remove the flag for the topic you need to assert. Note that `ignore` does not affect sending
messages from the test — only capture.

### Cause 2: `ignore-inbound` kicked in

The message was sent by the test itself, not by the application, and is hidden by
`ignore-inbound: true` (the default behavior).

**Fix:** if you need the whole stream including what the test sent, add a "monitor" connection with
`ignoreInbound: false`.

### Cause 3: the topic expression did not match

A `topics` key is a regular expression with full matching. For example, `order-.*` matches
`order-created`, but `order` does not.

**Fix:** check the expression; remember that the first match wins and the result is cached. Do not
make expressions overlap.

Data cannot be deserialized
---------------------------

### Symptoms

The Diff shows a "raw" record (key/value as `byte[]` in Base64) and an `<exception>` with a Jackson error.

### Cause

The topic `deserializer` does not match the data format the application sends, or `target-class` is
missing.

### Fix

Configure `deserializer` for the actual format (see [Serde](Serde.md)). If bad data is part of the
scenario under test, describe the error with `<exception>` (see
[Kafka Records](Records-and-Headers.md)).

Tests do not run without Docker
-------------------------------

### Symptoms

Kafka tests fail with Testcontainers errors, or the context does not start because the broker is
unavailable.

### Cause

Embedded Kafka is started through Testcontainers and requires a running Docker; the broker address is
injected from `${embedded.kafka.brokerList}`.

### Fix

Start Docker and make sure the configuration does not use a hardcoded broker address instead of
`${embedded.kafka.brokerList}`.

---
[← Back to Home](../README.md)
