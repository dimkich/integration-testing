Redis Push Messages (Pub/Sub)
=============================

[Russian version](../../ru/redis/Push-Messages.md)

Besides key changes, the framework captures Redis Pub/Sub pushes: `PUBLISH` and `SPUBLISH`
commands observed on the replication stream are turned into `RedisPush` messages, so they can
be asserted in `<outboundMessage>` and sent from a test with `<inboundMessage>`.

How It Works
------------

1. The background replication mechanism of the module (the same one that tracks keys) sees
   every `PUBLISH`/`SPUBLISH` command of the connection.
2. The command becomes a `RedisPush` message with the `channel` attribute and the deserialized
   `value` payload, and is stored for assertions.
3. A push sent by the test itself is published into Redis and filtered out of the captured
   messages by its transport identity (see [Deduplication](#deduplication)).

Pushes are captured from the moment the replication stream is active, including those published
before the first test (for example, by framework or library internals during application
startup). Technical channels that must not appear in assertions are hidden with `ignore: true`
(see [Ignoring Noise](#ignoring-noise)).

Direction 1: The Application Publishes a Push
---------------------------------------------

Call an application method that publishes, and describe the expected push in
`<outboundMessage>`:

```xml
<test type="Case" name="Order service publishes a notification">
    <bean>orderService</bean>
    <method>complete</method>
    <request>order-1</request>

    <outboundMessage type="RedisPush" connection="redissonConnectionFactory" channel="orders.events">
        <value type="OrderEvent">
            <orderId>order-1</orderId>
            <status>COMPLETED</status>
        </value>
    </outboundMessage>
</test>
```

`connection` is the Spring `RedisConnectionFactory` bean name the push was observed on. If
several factories point to the same Redis, the same push is captured once per factory; mark
technical channels with `ignore: true` (see below) if you do not want to assert every copy.

Direction 2: The Test Sends a Push to the Application
-----------------------------------------------------

Use `<inboundMessage>` to publish a push into Redis. The application handles it as a regular
Pub/Sub message; the echo of the test's own push never appears in `<outboundMessage>`:

```xml
<test type="Case" name="Application handles an inbound push">
    <test type="Part" name="Send">
        <inboundMessage type="RedisPush" connection="redissonConnectionFactory" channel="orders.in">
            <value type="OrderEvent">
                <orderId>order-2</orderId>
                <status>NEW</status>
            </value>
        </inboundMessage>
    </test>
    <test type="Part" name="SUT acknowledged">
        <bean>pushTestFacade</bean>
        <method>awaitOrder</method>
        <response type="OrderEvent">
            <orderId>order-2</orderId>
            <status>NEW</status>
        </response>
    </test>
</test>
```

The application processes a push asynchronously, so the framework cannot know when the SUT
side is done. When the test needs to observe the SUT reaction, await it explicitly (as in the
example above) before asserting.

Channel Serialization (`schemas`)
---------------------------------

Channels are matched against the regular key schemas of the connection: the same
longest-prefix rules, `defaultSchema` and `schemas.*` entries apply (see
[Configuration.md](Configuration.md#key-pattern-resolution-longest-prefix-match)). The
channel name itself is encoded and decoded with the connection's `keyCodec`, exactly like
Redis key names. The payload is serialized and deserialized with the `value` codec
of the matched schema; `ignore` and `excludedFields` work as well:

```yaml
integration:
  testing:
    redis:
      connections:
        redisConnectionFactory:
          defaultSchema:
            value:
              type: string      # channels not matching any prefix
          schemas:
            "orders.events":
              value:
                bean-ref: orderEventSerializer
            "internal.":
              ignore: true      # technical channels are not captured
```

The `excludedFields` of the matched schema are applied to the push `value`.

Deduplication
-------------

A captured push is filtered out when its identity matches a push sent by the test.
The identity consists of:

* the connection (factory bean name),
* the channel,
* the raw payload bytes.

The identities are not consumed: one push observed through several transports is filtered
everywhere. The flip side is that the SUT republishing exactly the same payload on the same
channel within one test is indistinguishable from the echo and is also filtered. There is no
`ignore-inbound` analogue for Redis: the test's own pushes are always hidden.

SPUBLISH
--------

Sharded pushes (`SPUBLISH`) are captured the same way and reported as regular `RedisPush`
messages; there is no separate message type.

Deserialization Errors
----------------------

If the payload cannot be deserialized with the channel's `value` codec, the push is still
captured: the raw bytes are kept in `<value type="byte[]">` (Base64 in XML) and the error is
described in `<exception>`:

```xml
<outboundMessage type="RedisPush" connection="redissonConnectionFactory" channel="orders.events">
    <value type="byte[]">eyJvcmRlcklkIjoib3JkZXItMSJ9</value>
    <exception>
        <exceptionType>IllegalArgumentException</exceptionType>
        <message>...</message>
    </exception>
</outboundMessage>
```

Wait Behavior
-------------

Replication is awaited via the wait-completion subsystem: after initialization (so data storage
snapshots and diffs are read from a caught-up mirror) and after the test action (so pushes and
mutations are captured before assertions). To do that, the module publishes a barrier token into
a technical channel and waits until the replication stream catches up with Redis. The barrier
channel (`__integration_testing_sync__`) itself is never captured; do not use it in the
application. This requires `integration.testing.wait.completion.enabled=true` (already set in
`redis.properties`); with an explicit `false` the module does not await replication.

Ignoring Noise
--------------

Technical channels (for example, internal Redisson channels) must be excluded with
`ignore: true` on the corresponding prefix; ignored channels are not captured and do not
appear in `<outboundMessage>`. See
[Troubleshooting.md](Troubleshooting.md#problem-push-messages-missing-from-outboundmessage)
if an unexpected push is missing from or appears in the assertions.

---
[← Back to Home](../README.md)
