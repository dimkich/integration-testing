Redis Integration Testing
=========================

[Russian version](../../ru/redis/README.md)

This module allows you to test your application's interaction with a Redis database. You can describe
test scenarios in XML or JSON files: write initial data to Redis before a test, invoke application
methods, and verify how the data in the database changed afterward.

How It Works
------------

You don't need to write manual assertions in Java. The framework does everything automatically:

1. **Data Preparation:** Before a test starts, the framework writes the data you specified in the
   `<init>` block to a real Redis instance.
2. **Test Execution:** Your test runs (e.g., a service method is called or a message is sent to
   a queue).
3. **Database Monitoring:** A special background mechanism instantly captures any changes your
   application makes in Redis (creating, modifying, or deleting keys, or changing their TTL).
4. **Result Verification:** At the end of the test, the framework compares the real database state
   with what you described in the `<dataStorageDiff>` block and displays the difference as a
   convenient visual diff if something went wrong.

Project Setup
-------------

To enable Redis tests, just add one annotation to your test class in Java:

```java
package com.example.redis;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.redis.EnableTestRedis;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

@EnableTestRedis // <-- This annotation enables Redis test support
@SpringBootTest
public class UserRedisIntegrationTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> redisTests() throws Exception {
        // Point to your XML test file
        return dynamicTestBuilder.build("tests/redis-user.xml");
    }
}
```

Quick Start: Writing Your First Test
-------------------------------------

Create the file `src/test/resources/tests/redis-user.xml`. In this example we will:

* Clear the database before the test.
* Write a user session with the value `ACTIVE`.
* Verify that the key appeared in the database.
* Call the delete method and confirm the key is gone from Redis.

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- @formatter:off -->
<test type="Container">
    <!-- Clear the Redis database before running the test -->
    <init type="keyValueStorageInit" name="redissonConnectionFactory" clear="true" />

    <test type="Case" name="Testing sessions in Redis">
        
        <test type="Part" name="Step 1: Creating a session">
            <!-- Call the set method on the redisStringFacade bean -->
            <bean>redisStringFacade</bean>
            <method>set</method>
            <request>user:session:100</request>
            <request>ACTIVE</request>
            
            <!-- Verify that the key was added to Redis -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="added">
                        <key>user:session:100</key>
                        <value type="RedisEntry">
                            <data>ACTIVE</data>
                        </value>
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>

        <test type="Part" name="Step 2: Verifying session read">
            <bean>redisStringFacade</bean>
            <method>get</method>
            <request>user:session:100</request>
            <!-- The method should return ACTIVE -->
            <response>ACTIVE</response>
        </test>

        <test type="Part" name="Step 3: Deleting the session">
            <bean>redisStringFacade</bean>
            <method>delete</method>
            <request>user:session:100</request>
            <!-- The delete method returned true (success) -->
            <response type="Boolean">true</response>
            
            <!-- Verify that the key was actually removed from Redis -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="deleted">
                        <key>user:session:100</key>
                        <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>
        
    </test>
</test>
```

Supported Redis Data Types
--------------------------

You can work with all major Redis data types:

| Redis Type | Description |
|-----------|-------------|
| **String** | Plain text |
| **Hash** | A set of field-value pairs (like folders with files). Each field can have its own TTL |
| **List** | An ordered list of elements where insertion order matters |
| **Set** | A collection of unique elements (no duplicates) |
| **Sorted Set** (ZSet) | Elements with a numeric score that determines sorting order |
| **Stream** | A message queue where each message has a unique ID (e.g., `1698400000000-0`) and a set of fields |
| **HyperLogLog** (HLL) | A structure for estimating unique element count (displayed as a regular unique set in tests) |

---
[← Back to Home](../README.md)
