In-Flight Tracking and Lag Control
==================================

[Russian version](../../ru/kafka/In-Flight-and-Lag.md)

Why it is needed
----------------

Working with Kafka is asynchronous: the application may have received a message but not finished
processing it, or sent a reply that the broker has not yet delivered to consumers. Finishing the case
right away would make the test compare results too early and become flaky.

That is why, before checking `<outboundMessage>`, the framework waits until all messages are actually
processed and only then compares the results. There are two modes for this wait.

Which one to choose
-------------------

There is nothing to configure: `inflight = true` works by default and is enough almost always. Switch
to `@EnableTestKafka(inflight = false)` if:

* the SUT runs in an external process or a Docker container — the application's clients are not
  visible from the test JVM;
* the test hangs because the application's consumer group is not visible to the framework.

The `inflight = true` mode (default)
------------------------------------

With a plain `@EnableTestKafka`, the framework itself tracks what the application sends and reads:

* how many messages were sent and which of them the broker has acknowledged;
* which offsets the application's consumers have committed;
* which topics and partitions consumers are subscribed to (including pattern subscriptions and manual
  assign);
* transactional sends: messages inside a transaction are counted only after its commit.

As long as at least one message remains unprocessed, lag is non-zero and the framework keeps waiting.

Mode limitations:

* it applies only to tests with `@EnableTestKafka(inflight = true)`;
* a consumer without `group.id` cannot be matched to a group and is not counted;
* when a transaction is aborted, its messages are not counted — just as a regular `read_committed`
  consumer would not see them;
* the mode only sees the application's clients in the same JVM: a group of an external process is not
  visible, use `inflight = false` for it.

The `inflight = false` mode (Admin API)
---------------------------------------

```java

@EnableTestKafka(inflight = false)
class LagMonitoringTest {
}
```

The framework does not watch the application's clients; instead it asks the broker about consumer
group state: it compares the log size (end offset) with the committed offsets. Lag exists while at
least one active group has not caught up.

Notes:

* groups in the EMPTY/DEAD state are not counted;
* if a group offset is not known yet (a freshly created topic), it is treated as zero — lag remains
  until the message is read;
* open transactions are not visible, so this mode is less accurate for transactional scenarios.

The main mode is `inflight = true`: it is faster because lag and group readiness are determined from
the application's client state, without contacting the broker. Use the Admin API mode only when the
application's clients are not available (or the SUT runs in an external process): every lag check
issues several broker requests, so waiting is slower. In the module tests the Admin API mode serves as
a "second opinion" — the same scenario runs in both modes.

How the framework waits for completion
--------------------------------------

1. **Application sends.** `@EnableTestKafka` waits for the sends made by the application within the
   case to complete.
2. **Startup stabilization.** Before every test the framework waits until consumer groups stop
   rebalancing (`startup-stabilization-timeout-seconds`, 30 seconds by default) and the groups listed
   in `expected-groups` appear with partitions assigned. If the expected groups are not ready by the
   timeout, the test fails with `Kafka startup stabilization timeout ...`. An empty listing alone does
   not block — waiting for groups only makes sense when `expected-groups` is configured (see
   [Configuration](Configuration.md)).
3. **Polling loop.** Then, every `lag-polling-interval-ms` (5 ms by default) the framework verifies
   that the sniffer is assigned to all partitions of interest and the expected groups are ready
   (readiness checks run no more often than once per `readiness-polling-interval-ms`). While the
   sniffer or the application is not ready, "lag cleared" is not treated as completion. Otherwise, lag
   is checked until it clears or `lag-polling-timeout-ms` (10 seconds by default) expires.
4. **Result.** As soon as lag clears and all consumers are ready, the case continues. If time runs
   out, the case fails with:

```text
Kafka wait completion timeout. Unprocessed messages remaining on broker.
```

When the cause is an unready sniffer or expected groups, the message is different, for example
`Kafka wait completion timeout. expected consumer groups are not ready: ...`.

If this error keeps happening, increase `lag-polling-timeout-ms` — for example, when the application
processes messages in batches or calls an external service.

If the application deliberately leaves a failed message uncommitted (rollback, manual ack, a skipped
poison record), a longer timeout will not help: the lag never clears. Use
[Error Scenarios and Uncommitted Offsets](Error-Handling.md) instead.

Sniffer topic reading
---------------------

* the sniffer reads all topics except internal ones (their names start with `__`);
* on first connect, the topic's old history is skipped — only new messages are read;
* a topic created during the test is read from the very beginning — that is why the "Cold Start"
  scenario works without any special setup;
* pattern subscriptions (`topics` with a regular expression) are supported just like exact topics;
* if the application assigns partitions manually (`assign`) instead of subscribing, that is tracked as
  well.

Timeout configuration
---------------------

```yaml
integration:
  testing:
    kafka:
      # pause between lag checks
      lag-polling-interval-ms: 5
      # total time to wait for lag to clear
      lag-polling-timeout-ms: 10000
      # pause between readiness checks of expected groups
      readiness-polling-interval-ms: 100
      # wait for consumer group stabilization at startup
      startup-stabilization-timeout-seconds: 30
```

Mode comparison
---------------

|                                   | `inflight = true` (default)            | `inflight = false`                                                                                       |
|-----------------------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------|
| How lag is determined             | from the application's clients         | through the broker Admin API                                                                             |
| How group readiness is determined | from the application's client state    | through the Admin API                                                                                    |
| Transactions                      | counted accurately (only after commit) | not visible                                                                                              |
| SUT in another JVM                | not supported                          | supported                                                                                                |
| Check speed                       | high                                   | lower: broker requests on every check                                                                    |
| When to choose                    | the main mode                          | only when the application's clients are not available (or the scenario should be checked "from outside") |

Scenarios that exercise this
----------------------------

| Scenario         | What it verifies                                                          |
|------------------|---------------------------------------------------------------------------|
| Slow SUT         | the application replies with a delay — the test does not finish too early |
| Cold Start       | the topic is created mid-test — early messages are not lost               |
| Pattern Matching | the application subscribes by pattern — responses are collected           |
| Transactions     | commit/abort and offset commit — lag counts only committed data           |
| Assign           | manual partition assignment — messages are still tracked                  |

---
[← Back to Home](../README.md)
