Error Scenarios and Uncommitted Offsets
=======================================

[Russian version](../../ru/kafka/Error-Handling.md)

Why the test may fail on a message the application "skipped"
------------------------------------------------------------

Before assertions, the framework waits until all messages are processed. In the `inflight = true`
mode a message counts as processed only when the consumer group has committed an offset past it:
while `endOffset > committedOffset` for at least one subscribed partition of a live group, lag is
non-zero. The `inflight = false` mode asks the broker the same question through the Admin API.

The wait is bounded by `lag-polling-timeout-ms` (10000 ms by default; do not confuse it with
`startup-stabilization-timeout-seconds`, 30 s — that timeout awaits consumer-group readiness before
every test). When the time is up, the case fails:

```text
Kafka wait completion timeout. Unprocessed messages remaining on broker.
```

If the application handles the failure but does not commit the offset of the skipped message
(`AckMode.MANUAL` without `acknowledge()`, manual commit on error, a test of the rollback logic, and
so on), the lag never clears by itself. There are two ways out.

Path 1. Make the application commit the skipped message (recommended)
---------------------------------------------------------------------

The application should commit the offset of a record it has decided to skip — otherwise the
consumer never moves on and the broker keeps showing lag for it.

Spring Kafka, regular listener:

```java

@Bean
DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
    DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template));
    handler.setCommitRecovered(true);
    return handler;
}
```

`commitRecovered = true` tells Spring Kafka to commit the offset of a recovered (for example,
dead-lettered) record. It is honored with `AckMode.MANUAL_IMMEDIATE`; with other ack modes the
container commits after error handling unless `setAckAfterHandle(false)` is set. Check both flags if
the lag persists.

Spring Kafka, transactional (EOS) listener:

```java

@Bean
DefaultAfterRollbackProcessor<Object, Object> rollbackProcessor(KafkaTemplate<Object, Object> template) {
    return new DefaultAfterRollbackProcessor<>(
            new DeadLetterPublishingRecoverer(template),
            SeekUtils.DEFAULT_BACK_OFF,
            template,
            true);
}
```

`DefaultAfterRollbackProcessor` is invoked when a transactional container rolls back. With
`commitRecovered = true` it submits the skipped offset with `sendOffsetsToTransaction`, so a
transactional `KafkaOperations` is required — otherwise the flag is ignored. For a non-transactional
application use `DefaultErrorHandler` above.

After that the framework sees the committed offset, and the case completes without delays.

Path 2. Escape hatch: force a commit from the test
--------------------------------------------------

Sometimes leaving lag is the expected behavior:

* the test verifies rollback or manual commit logic;
* the application deliberately does not ack a failed record;
* the application configuration cannot be changed right now.

Then intercept the completion of Spring Kafka's error handling and commit the offset from the test.
`handleRemaining` runs on the consumer thread, so `commitSync` is safe there. The framework learns
about the commit from the standard `ConsumerInterceptor.onCommit`, and in `inflight = false` mode the
broker reports it through the Admin API.

### 1. Action

A `MethodAction` is a plain Java class with a public no-args constructor (it is not a Spring bean).

```java
package com.example.test;

import io.github.dimkich.integration.testing.execution.hook.MethodAction;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class KafkaForceCommitAction implements MethodAction {

    private static final Logger log = Logger.getLogger(KafkaForceCommitAction.class.getName());

    @Override
    @SuppressWarnings("unchecked")
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        // DefaultErrorHandler.handleRemaining(Exception, List<ConsumerRecord<?, ?>>, Consumer<?, ?>, MessageListenerContainer)
        List<ConsumerRecord<?, ?>> records = (List<ConsumerRecord<?, ?>>) args[1];
        Consumer<?, ?> consumer = (Consumer<?, ?>) args[2];
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        for (ConsumerRecord<?, ?> record : records) {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());
            offsets.merge(partition, new OffsetAndMetadata(record.offset() + 1),
                    (left, right) -> left.offset() >= right.offset() ? left : right);
        }
        try {
            consumer.commitSync(offsets);
        } catch (Exception e) {
            log.fine("Force commit failed: " + e);
        }
    }
}
```

The action commits the maximum `offset + 1` per partition over all records passed to the handler:
this skips the whole failed poll, and the next test does not inherit the lag. Committing only the
first record is not enough: a later message in the same partition keeps lag non-zero.

### 2. Annotation

```java

@EnableTestKafka
@SpringBootTest
@OnMethodExit(
        pointcut = "t.inherits('org.springframework.kafka.listener.CommonErrorHandler') && m.name('handleRemaining')",
        action = KafkaForceCommitAction.class)
public abstract class BaseKafkaIntegrationTest {
    // ...
}
```

`@OnMethodExit` is `@Inherited`, so it can be placed on a base test class. The pointcut covers any
`CommonErrorHandler`, including `DefaultErrorHandler`.

### How it works at runtime

1. The test sends a message the application fails to process. The framework sees the lag and waits.
2. The application exhausts its retries and passes the record to the error handler.
3. On exit from `handleRemaining` the action calls `consumer.commitSync(...)` on the consumer
   thread.
4. Kafka reports the successful commit to `ConsumerInterceptor.onCommit`; the in-flight ledger (or
   the Admin API in `inflight = false` mode) sees the new committed offset.
5. On the next lag poll (`lag-polling-interval-ms`, 5 ms by default) the framework sees zero lag and
   continues the case. The broker is left clean for the following tests.

### Limitations

* The message is skipped for the group — the application will not process it. Use the escape hatch
  only when the scenario expects this.
* Do not use it for transactional (EOS) listeners: a direct consumer commit mixed with
  `sendOffsetsToTransaction` breaks the transaction contract. Fix the transaction logic instead.
* For batch listeners the interception point is `handleBatch`, and `args[1]` is
  `ConsumerRecords<?, ?>`, not a record list.
* Action errors do not fail the application: `MethodHookAdvice` logs them at `FINE`.
* If the hook does not fire because the error handler class was loaded earlier, add
  `@RepeatInstrumentation({"org.springframework.kafka"})` — see
  [Method Hooks](../Method-Hooks.md#troubleshooting).

Runnable example
----------------

The module tests contain a complete scenario:

* `integration-testing-kafka/src/test/java/io/github/dimkich/integration/testing/kafka/uncommitted/KafkaForceCommitAction.java`
* `integration-testing-kafka/src/test/java/io/github/dimkich/integration/testing/kafka/uncommitted/UncommittedOffsetConfig.java`
* `integration-testing-kafka/src/test/java/io/github/dimkich/integration/testing/kafka/uncommitted/SutErrorHandlingExamples.java`
  — the Path 1 configuration (compile-checked, not part of the test context)
* `integration-testing-kafka/src/test/java/io/github/dimkich/integration/testing/kafka/KafkaInFlightTest.java`
  and `KafkaAdminTest.java` — the `@OnMethodExit` declaration; both run `kafka.xml`
* `integration-testing-kafka/src/test/resources/kafka.xml` — the cases "Poison message: offset is
  force-committed by the hook" and "Next case is not blocked by leftover lag"

See also
--------

* [In-Flight Tracking and Lag Control](In-Flight-and-Lag.md) — how lag is calculated and which
  timeouts apply.
* [Method Hooks](../Method-Hooks.md) — the `@OnMethodExit` / `MethodAction` mechanism.
* [Troubleshooting](Troubleshooting.md) — other causes of the wait timeout.

---
[← Back to Home](../README.md)
