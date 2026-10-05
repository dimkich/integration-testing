Kafka Integration Testing
=========================

[Russian version](../../ru/kafka/README.md)

This module lets you test how your application works with Kafka. Scenarios are described in XML or
JSON: the test publishes messages to topics, runs application logic and verifies what the application
sent in response.

How does it work?
-----------------

The framework takes care of the broker side completely:

1. **Sending messages:** before the case, the framework publishes messages from `<inboundMessage>`
   blocks to the specified topics.
2. **Running the test:** your scenario runs — a bean method call or an HTTP request. When the case is
   triggered by an `<inboundMessage>`, this step is just waiting for processing.
3. **Watching topics:** a background consumer (sniffer) reads all user topics and picks up everything
   the application sends.
4. **Checking results:** at the end of the case, the framework compares the captured messages with
   `<outboundMessage>` blocks and shows the difference as a visual Diff.

The framework also waits until the application has actually processed all messages before finishing
the case — see [In-Flight Tracking and Lag Control](In-Flight-and-Lag.md).

Connecting to your project
--------------------------

Kafka is started automatically via Testcontainers, so Docker is required to run the tests. All your
test class needs is:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.kafka.EnableTestKafka;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

@EnableTestKafka // <-- This annotation enables Kafka test support
@SpringBootTest
public class OrderKafkaIntegrationTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> kafkaTests() throws Exception {
        // Point the builder to your XML test file
        return dynamicTestBuilder.build("tests/kafka-order.xml");
    }
}
```

`@EnableTestKafka` activates the wait-completion subsystem on its own: the
`integration.testing.wait.completion.enabled` property
is already defined in `kafka.properties`, so you do not need to set it in `@SpringBootTest`.

The only thing left is to set the broker address in `application-test.yml`. Embedded Kafka publishes
it in the `embedded.kafka.brokerList` property:

```yaml
spring:
  kafka:
    bootstrap-servers: ${embedded.kafka.brokerList}   # application clients

integration:
  testing:
    kafka:
      connections:
        kafka1:
          bootstrapServers: ${embedded.kafka.brokerList}   # framework clients
```

> **Tip:** the default `inflight = true` mode is the main and fastest one: lag is computed from the
> application's clients without contacting the broker. Use
> `@EnableTestKafka(inflight = false)` (Admin API) only when the application's clients are not
> available (for example, the SUT runs in another process) — it queries the broker on every lag check
> and is therefore slower. See [In-Flight Tracking and Lag Control](In-Flight-and-Lag.md) for details.

Quick start: writing your first test
------------------------------------

Create the file `src/test/resources/tests/kafka-order.xml`. The example uses the default serde
settings (string key/value), so no extra topic configuration is needed:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- @formatter:off -->
<test type="Container">
    <!-- Case 1: the application is triggered by a Kafka message -->
    <test type="Case" name="Order processed">
        <inboundMessage type="KafkaRecord" connection="kafka1" topic="order-in">
            <key>order-1</key>
            <value>{"orderId": 1}</value>
        </inboundMessage>

        <!-- Verify that the application sent the result -->
        <outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
            <key>order-1</key>
            <value>{"orderId": 1, "status": "PROCESSED"}</value>
        </outboundMessage>
    </test>

    <!-- Case 2: the application is triggered by a method call -->
    <test type="Case" name="Order processed via service call">
        <bean>orderService</bean>
        <method>processOrder</method>
        <request>order-2</request>

        <outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
            <key>order-2</key>
            <value>{"orderId": 2, "status": "PROCESSED"}</value>
        </outboundMessage>
    </test>
</test>
```

How to read this test:

* a case is triggered **either** by a message **or** by a method call — these are two mutually exclusive
  ways to start it:
  * `<inboundMessage>` — a message the framework publishes to Kafka, which the application listener
    processes;
  * `<bean>`/`<method>`/`<request>` — a regular call to an application bean method;
* if a single case has both `<inboundMessage>` and `<bean>`/`<method>`, the method call is ignored — the
  framework only sends the message;
* `<outboundMessage>` — a message the application must send to the `order-out` topic.

The `<outboundMessage>` comparison runs after the framework makes sure processing has finished, so no
`Thread.sleep` is needed in tests.

What the module can do
----------------------

| Capability                                                           | Where to read                                                |
|----------------------------------------------------------------------|--------------------------------------------------------------|
| Sending messages from the test and asserting application responses   | [Kafka Records](Records-and-Headers.md)                      |
| Headers, including repeated ones, partitions and time                | [Kafka Records](Records-and-Headers.md)                      |
| Tombstone records (key without value)                                | [Kafka Records](Records-and-Headers.md)                      |
| Serde setup: String, JSON, XML, bytes, Spring JSON/XML               | [Serde](Serde.md)                                            |
| Multiple broker connections, topic regex patterns                    | [Configuration](Configuration.md)                            |
| Waiting until the application processes all messages                 | [In-Flight Tracking and Lag Control](In-Flight-and-Lag.md)   |
| Topics created during the test, pattern subscriptions, manual assign | [In-Flight Tracking and Lag Control](In-Flight-and-Lag.md)   |
| Transactions: commit, abort, offset commit                           | [In-Flight Tracking and Lag Control](In-Flight-and-Lag.md)   |
| Failed messages and uncommitted offsets                              | [Error Scenarios and Uncommitted Offsets](Error-Handling.md) |
| Custom formats and serializers                                       | [Extensibility](Extensibility.md)                            |
| Diagnosing typical errors                                            | [Troubleshooting](Troubleshooting.md)                        |

---
[← Back to Home](../README.md)
