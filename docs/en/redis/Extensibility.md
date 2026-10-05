Customizing and Extending the Framework via Java
=================================================

[Russian version](../../ru/redis/Extensibility.md)

The `integration-testing-redis` module is designed as an extensible system. If your
application uses proprietary Redis clients, custom serialization protocols, or specific
database modules (e.g., RedisJSON), you can easily teach the framework to work with them.

All custom components are registered as standard Spring beans (`@Component` or `@Bean`)
and are automatically wired into the framework's data processing pipeline.

Custom Serialization Formats
----------------------------

If the standard core providers (`string`, `bytes`, `json`, ...) are insufficient, a component
source can be your own Spring Data `RedisSerializer` or Redisson `Codec`, or you can write a
custom `TestSerdeAdapter` — see the "Source Adapters" section below. `RedisDataCodec` is an
internal framework class (a pair of serde converters); there is no need to implement it.

### Custom Serializer (Spring Data `RedisSerializer`)

A serializer converts Java objects to and from bytes. For example, Snappy-compressed strings:

```java
package com.example.redis.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;
import org.xerial.snappy.Snappy;

import java.nio.charset.StandardCharsets;

public class SnappyStringSerializer implements RedisSerializer<Object> {

    @Override
    public byte[] serialize(@Nullable Object object) {
        if (object == null) return null;
        try {
            return Snappy.compress(object.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Data compression error", e);
        }
    }

    @Override
    public Object deserialize(@Nullable byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            return new String(Snappy.uncompress(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Data decompression error", e);
        }
    }
}
```

### Schema Component from a Custom Serializer

A schema is assembled from three components (`value`/`hash-key`/`hash-value`), and each of them can
use your serializer as its source. For example, compress values with Snappy and keep hash
field keys as plain strings:

```yaml
connections:
  redisConnectionFactory:
    schemas:
      "compressed:":
        value:
          type: "com.example.redis.serializer.SnappyStringSerializer"
        hash-key:
          type: string
        hash-value:
          type: "com.example.redis.serializer.SnappyStringSerializer"
```

Custom Data Persistence Strategies (`RedisDataAccessor`)
--------------------------------------------------------

If you have created a custom data type implementing the `RedisValue` interface (e.g.,
`CustomBloomFilter`), the framework needs instructions on how to write it to Redis.
Implement `RedisDataAccessor<D>` for this purpose.

### Example: Accessor for a Custom Type

```java
package com.example.redis.accessor;

import io.github.dimkich.integration.testing.redis.accessor.RedisDataAccessor;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;
import io.github.dimkich.integration.testing.redis.model.RedisValue;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;

@Component  // Automatically registered in RedisAccessorCoordinator
public class BloomFilterAccessor implements RedisDataAccessor<CustomBloomFilter> {

    @Override
    public Class<? extends RedisValue> getSupportedClass() {
        return CustomBloomFilter.class;
    }

    @Override
    public void store(byte[] key, CustomBloomFilter value,
                      RedisConnection conn, RedisDataSchema schema) {
        conn.set(key, schema.getValueCodec().serialize(value));
    }
}
```

### Registering a `RedisValue` Subtype in Jackson

For the new type to be properly deserialized from XML/JSON, register it in
`TestSetupModule`:

```java

@Bean
TestSetupModule testSetupModule() {
    return new TestSetupModule()
            .addSubTypes(CustomBloomFilter.class, "CustomBloomFilter");
}
```

After this, you can use `CustomBloomFilter` in XML tests by its short name.

Source Adapters (`TestSerdeAdapter`)
------------------------------------

Your application may already have complex Redis clients configured (e.g., proprietary
Jedis wrappers or third-party clients) that hold serialization rules internally. To avoid
duplicating their code in tests, write an **adapter** that converts a native single-slot source
(e.g., a Spring Data `RedisSerializer` or a Redisson `Codec`) into the serde contract — an
"object ↔ byte[]" pair. The general adapter selection rules (class hierarchy traversal,
candidate order, `null` = "not mine") are implemented by
`io.github.dimkich.integration.testing.serde.AdapterManager`.

The adapter implements `TestSerdeAdapter<S, I, O, C, R, P>` and returns an `input -> output` converter
built via `TestSerdeConverter.of(...)`.

### Example: Adapter for a Custom Serializer

Suppose your application declares a custom serializer bean:

```java
public class CustomSerializer {
    public byte[] write(Object value) { ... }
    public Object read(byte[] data) { ... }
}
```

Implement a serialization adapter:

```java
package com.example.redis.adapter;

import com.example.CustomSerializer;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component // The adapter is picked up by the adapter manager automatically
@Getter
public class CustomSerializerAdapter
        implements TestSerdeAdapter<CustomSerializer, Object, byte[], TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<CustomSerializer> sourceClass = CustomSerializer.class;

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, byte[], TestSerdeContext> adapt(
            CustomSerializer source, StandardSerdeProperties properties, Class<Object> inputClass,
            Class<byte[]> outputClass, Class<TestSerdeContext> contextClass, ComponentRole role) {
        if (!byte[].class.equals(outputClass)) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> source.write(input));
    }
}
```

A symmetric `byte[] -> Object` adapter calls `source.read(...)`. Once registered, the bean can be
referenced from a schema component:

```yaml
connections:
  redisConnectionFactory:
    defaultSchema:
      value:
        bean-ref: "customSerializerBean"
```

### Redisson codec slots

When a schema component references a Redisson `Codec` (directly or via `RedissonClient`), the core
passes the slot role to the adapter, and the adapter extracts the matching encoder/decoder pair:
for `value` — `ValueEncoder`/`ValueDecoder`, for `hash-key` — `MapKeyEncoder`/`MapKeyDecoder`, for
`hash-value` — `MapValueEncoder`/`MapValueDecoder`. That is why the same bean can be referenced
from all three schema components, and hash fields are serialized with their own codecs:

```yaml
connections:
  redisConnectionFactory:
    schemas:
      "cache:":
        value:
          bean-ref: "redissonClient"
        hash-key:
          bean-ref: "redissonClient"
        hash-value:
          bean-ref: "redissonClient"
```

Custom Binary Envelope Tags
-------------------------

Custom tags (`BinarySegmentProvider`/`BinarySegment`) are a shared capability of the serde core:
implement the segment and its provider and register the provider as a `@Component`. An example of
creating the `{XORMASK(mask)}` tag is described in
[Serde Extensibility](../serde/Extensibility.md#custom-binary-envelope-tags-binarysegmentprovider-binarysegment).


Custom Replication Command Handlers (`RedisSnapshotHandler`, `RedisStreamHandler`)
---------------------------------------------------------------------------------

If your application uses complex Redis commands or specific database modules (e.g.,
RedisJSON), the background replication thread will fail with an
`UnsupportedOperationException` when encountering such events.

You can teach the replicator to handle these events by writing custom handlers.

### Custom Command Parser (`NamedCommandParser`)

If Redis executes a non-standard or new command not present in the `redis-replicator`
library, register a command parser for it:

```java
package com.example.redis.parser;

import com.moilioncircle.redis.replicator.cmd.impl.AbstractCommand;
import io.github.dimkich.integration.testing.redis.replication.NamedCommandParser;
import org.springframework.stereotype.Component;

@Component // The replicator will automatically pick up this parser on startup
public class MyCustomCommandParser implements NamedCommandParser<MyCustomCommand> {

    @Override
    public String getCommandName() {
        return "MYCUSTOMCMD"; // Command name in uppercase
    }

    @Override
    public MyCustomCommand parse(Object[] command) {
        byte[] key = (byte[]) command[1];
        byte[] value = (byte[]) command[2];
        return new MyCustomCommand(key, value);
    }
}
```

Where `MyCustomCommand` is your command class extending `AbstractCommand`.

### Live Stream Replication Handler (`RedisStreamHandler`)

Once the command is parsed, it must be applied to the local in-memory database mirror
(`RedisInMemoryStore`):

```java
package com.example.redis.handler;

import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import com.example.parser.MyCustomCommand;
import org.springframework.stereotype.Component;

@Component // The event dispatcher will automatically register this handler
public class MyCustomCommandHandler implements RedisStreamHandler<MyCustomCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == MyCustomCommand.class;
    }

    @Override
    public void handle(MyCustomCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            entry.setValue(schema, event.getValue());
        });
    }
}
```

### RDB Snapshot Handler (`RedisSnapshotHandler`)

If a custom data type needs to be supported not only in the command stream but also loaded
during the initial full database snapshot (RDB), implement a snapshot event handler:

```java
package com.example.redis.handler;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueModule;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import org.springframework.stereotype.Component;

@Component
public class MyModuleSnapshotHandler implements RedisSnapshotHandler<KeyStringValueModule> {

    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueModule.class;
    }

    @Override
    public void handle(KeyStringValueModule event, RedisInMemoryStore store) {
        store.compute(event, RedisHash.class, (schema, hash) -> {
            byte[] rawValue = event.getValue();
            // Your module unpacking logic and hash population...
            hash.putMapEntry("status", "LOADED_FROM_MODULE");
        });
    }
}
```

All custom snapshot handlers (`RedisSnapshotHandler`) and stream handlers
(`RedisStreamHandler`) are automatically collected by the `snapshotDispatcher` and
`streamDispatcher` dispatchers, completely eliminating the problem of unsupported data
types during tests.

---
[← Back to Home](../README.md)
