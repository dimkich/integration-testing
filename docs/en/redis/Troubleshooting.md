Troubleshooting Common Redis Test Issues
=========================================

[Russian version](../../ru/redis/Troubleshooting.md)

When writing integration tests with Redis, you may encounter typical configuration errors,
data type mismatches, or test hangs. This guide covers the most common problems and their
solutions.

Error: Method not found in bean `<bean_name>`
---------------------------------------------

### Symptoms

Your test fails when calling a Spring bean method with the following log message:

```
java.lang.RuntimeException: Method rPush not found in bean redisStringFacade
```

### Cause 1: Missing type for non-string arguments in XML

By default, all `<value>` and `<request>` tags in XML without an explicit `type` attribute
are treated by the framework as strings (`String`). If your Java method accepts `Integer`,
`Long`, or `Boolean`, Java reflection cannot match the types.

**How to fix:** Always explicitly specify types for numbers and booleans:

```xml
<!-- ❌ Incorrect (looks for a method with a String parameter) -->
<request>10</request>

        <!-- ✅ Correct -->
<request type="Integer">10</request>
```

### Cause 2: Wrong type for variable-length arguments (Varargs)

If you are calling a method with varargs (e.g., `String... values`), passing a list of
elements as a regular `ArrayList` will cause an error because Java reflection expects
a strict `Object[]` array.

**How to fix:** Wrap the list of arguments in the `Object[]` type:

```xml
<!-- Method: public Long rPush(String key, String... values) -->
<request>my:list:key</request>
        <!-- ✅ Explicitly wrap varargs in an array -->
<request type="Object[]">
<item>value_1</item>
<item>value_2</item>
</request>
```

Error: No handler for `<Event_Class>`
--------------------------------------

### Symptoms

When executing an operation in Redis, the background replicator fails with a critical error
that is then propagated to your test:

```
java.lang.UnsupportedOperationException: No handler for com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueModule
```

### Cause and Solution

The background network replicator encountered an event in the incoming data stream for which
no handler is registered in the test environment. This most often occurs when using third-party
Redis modules (e.g., writing JSON documents via RedisJSON, which generates
`KeyStringValueModule` events) or when writing complex system objects.

Since the replicator does not know how to parse the structure of this event, it cannot
continue and terminates the replication stream with an error.

**How to fix:** To support a new event type, implement a corresponding handler in your test
Java code — `RedisSnapshotHandler` (if the event occurs during RDB snapshot reading) or
`RedisStreamHandler` (if the event occurs during live command stream reading). A detailed
example of writing such a handler is provided in the `Extensibility.md` guide.

Error: Test hangs or Redis Sync Timeout
----------------------------------------

### Symptoms

The test starts but "hangs" for several seconds, then fails with the following error:

```
java.lang.RuntimeException: Redis Sync Timeout/Error for [redissonConnectionFactory] using PUBLISH
```

### Cause and Solution

The blocking sync barrier (`RedisSyncBarrier`) sent a marker Pub/Sub `PUBLISH` command to
Redis, but the background replication thread could not read it from the network within the
allotted time (default 5 seconds).

**How to fix:**

1. **Check the console logs above the error:** If the background replication thread failed
   (e.g., due to an incorrect password or authentication error), the barrier will catch
   this error and print its real stack trace to the console. Fix the root cause (e.g.,
   provide the correct password in `application-test.yml`).
2. **Check that the database is running:** Ensure the Docker Redis container started
   successfully and is responding to requests.
3. **Increase the timeout:** If the database is overloaded or you are running tests on a
   slow machine (e.g., in CI/CD), increase the barrier timeout in `application-test.yml`:

```yaml
integration:
  testing:
    redis:
      # Increase sync timeout to 15 seconds
      syncBarrierTimeoutMs: 15000
```

Pub/Sub Channel Conflict
------------------------

### Symptoms

Redis tests pass in isolation but start failing with `Redis Sync Timeout` when new Pub/Sub
subscriptions are added to the project.

### Cause

The framework uses the reserved Pub/Sub channel `__integration_testing_sync__` for the
sync barrier handshake. If your application (or a third-party library) subscribes to the
same channel, the barrier messages may be intercepted and never reach the replicator.

### Solution

Ensure that neither your application code nor your test scenarios subscribe to the
`__integration_testing_sync__` channel. If necessary, rename the channel in your code,
avoiding the `__integration_testing_` prefix.

Problem: Database changes not showing in dataStorageDiff (empty diff)
----------------------------------------------------------------------

### Symptoms

The application ran successfully and wrote data to Redis, but the `dataStorageDiff` block
shows no database changes (or shows an empty block).

### Cause 1: Connection name mismatch in YAML

You configured serialization schemas for one connection name, but your application uses
a different one.

**How to fix:** Ensure that the subsection name in the `connections` block of your YAML
configuration file exactly matches the Spring `RedisConnectionFactory` bean name of your
application (see `Configuration.md` for details).

### Cause 2: Key matched an ignore filter (`ignore: true`)

Your key's prefix may have matched one of the masks for which ignoring is configured
in YAML.

**How to fix:** Check the `schemas` section in `application-test.yml`. Make sure your
business keys do not fall under the technical key masks you decided to hide.

### Cause 3: Multiple database mode is not enabled

Your application executes `SELECT 1` (writes to database index 1), but the `multipleDatabases`
parameter is not enabled in the connection settings. In this case, the replicator treats all
keys as belonging to database 0 and finds no changes.

**How to fix:** Enable multiple database support in the connection properties:

```yaml
connections:
  redisConnectionFactory:
    multipleDatabases: true   # <-- Enables correct key separation by database
```

---
[← Back to Home](../README.md)
