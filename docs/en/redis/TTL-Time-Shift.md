Testing Time-to-Live (TTL) and Time Shifts
===========================================

[Russian version](../../ru/redis/TTL-Time-Shift.md)

Testing data expiration logic (e.g., user sessions, temporary one-time authorization codes,
or caches) is often difficult. If a key is written to Redis with a TTL of 60 seconds, the
test cannot wait a full minute of real time — that would slow down the entire build pipeline.

The framework provides built-in support for virtual time and automatic purging of expired
data, allowing you to instantly verify key deletion on the fly.

The Time Problem in Testcontainers and Its Solution
---------------------------------------------------

When you use virtual time (the `@MockJavaTime` annotation on the test class and the
`DateTimeInit` initializer in XML), the framework advances the clock only inside the JVM
of your test.

However, the real Redis server (running in a Docker container or as an embedded process)
lives outside the JVM and continues to run according to its container's real system time.
It has no knowledge that time has moved forward in your test.

### How the Framework Solves This

On every time shift in your XML test (e.g., via `addDuration="PT10S"`):

1. The framework updates its internal virtual clock.
2. A background analyzer walks all keys in the local in-memory database mirror and computes
   which keys have a relative TTL less than or equal to zero relative to the new virtual
   time.
3. For all expired keys, the framework instantly sends commands to the real Redis to
   physically delete them (`DEL` for keys or `HDEL` for individual hash fields).
4. **Result:** The real Redis database is synchronized with your virtual time. If your
   application tries to read this key at the same moment, Redis returns `null` (key not
   found).

Managing Time in XML (`DateTimeInit`)
--------------------------------------

Time can be managed at any level of the test hierarchy using the `DateTimeInit`
initialization element.

### Option A: Setting Absolute Time

Used in the root container to set a single starting point for all tests.

```xml
<init type="dateTimeInit" dateTime="2026-01-01T12:00:00Z" />
```

### Option B: Relative Time Shift

Used inside test steps (`TestPart`) to simulate the passage of time.

```xml
<!-- Apply a 15-second time shift only for steps in the current case -->
<init type="dateTimeInit" applyTo="TestPart" addDuration="PT15S"/>
```

The `addDuration` format follows the ISO-8601 standard:

| Example   | Description        |
|-----------|--------------------|
| `PT15S`   | 15 seconds         |
| `PT5M`    | 5 minutes          |
| `PT2H30M` | 2 hours 30 minutes |
| `P1D`     | 1 day              |

TTL at the Hash Field Level (`HEXPIRE`)
----------------------------------------

Starting with Redis 7.4, you can set TTL for individual fields within a single hash
(commands `HEXPIRE` / `HPEXPIREAT`).

In an XML test, you can specify the lifetime individually for each hash field. When time
is shifted, the framework removes only those fields whose TTL has expired, leaving the
rest untouched.

### Describing a Hash with Per-Field TTL in XML:

```xml
<entry key="user:session:active" type="RedisEntry">
    <data type="RedisHash">
        <!-- Field without TTL (permanent) -->
        <entry>
            <key>username</key>
            <value type="RedisEntry">
                <data>Ivan</data>
            </value>
        </entry>
        <!-- Field with its own TTL (30 seconds) -->
        <entry>
            <key>one_time_token</key>
            <value type="RedisEntry">
                <data>token_9941</data>
                <ttl>PT30S</ttl> <!-- Only this field will be removed after 30 seconds -->
            </value>
        </entry>
    </data>
</entry>
```

Complete Practical TTL Test Scenario
-------------------------------------

Below is a ready-to-use XML scenario demonstrating key expiration testing.

### Scenario Description

1. **Step 1:** Write the key `user:session:temp` with a TTL of 15 seconds. Verify its
   creation and TTL in the database.
2. **Step 2:** Advance time by 10 seconds. The key should remain in Redis, but its TTL
   should decrease to 5 seconds.
3. **Step 3:** Advance time by another 10 seconds (20 seconds total since creation).
   The key should be physically deleted from Redis, and attempting to read it should
   return `null`.

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- @formatter:off -->
<test type="Container">
    <!-- Set a fixed starting time for the entire file -->
    <init type="DateTimeInit" dateTime="2026-05-24T12:00:00.000Z" />
    <init type="KeyValueStorageInit" name="redissonConnectionFactory" clear="true" />

    <test type="Case" name="Testing session expiration by TTL">
        <!-- Configure a 10-second time shift on each step (Part) -->
        <init type="DateTimeInit" applyTo="TestPart" addDuration="PT10S"/>

        <test type="Part" name="1. Writing a session with TTL 15 seconds">
            <bean>redisStringFacade</bean>
            <method>setEx</method>
            <request>user:session:temp</request>
            <request>ACTIVE_SESSION</request>
            <request type="Long">15</request>
            <response />
            
            <!-- Expect the key to appear with a TTL of 15 seconds -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="added">
                        <key>user:session:temp</key>
                        <value type="RedisEntry">
                            <data>ACTIVE_SESSION</data>
                            <ttl>PT15S</ttl>
                        </value>
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>

        <test type="Part" name="2. Time shift by 10 seconds (key exists, TTL decreased)">
            <!-- Time shifted by +10s (10s from start). Key lives for another 5s -->
            <bean>redisStringFacade</bean>
            <method>get</method>
            <request>user:session:temp</request>
            <response>ACTIVE_SESSION</response>
            
            <!-- Verify that the TTL in the database decreased to 5 seconds -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="changed">
                        <key>user:session:temp</key>
                        <value type="EntriesObjectKeyObjectValue">
                            <entry change="changed">
                                <key>ttl</key>
                                <value type="PeriodDuration">PT5S</value>
                            </entry>
                        </value>
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>

        <test type="Part" name="3. Time shift by another 10 seconds (key should be deleted)">
            <!-- Time shifted by another +10s (20s from start). Key fully expired. -->
            <bean>redisStringFacade</bean>
            <method>get</method>
            <request>user:session:temp</request>
            <!-- The get method should return null (session deleted) -->
            <response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
            
            <!-- Verify that the key was physically removed from the store -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="deleted">
                        <key>user:session:temp</key>
                        <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>
    </test>
</test>
```

---
[← Back to Home](../README.md)
