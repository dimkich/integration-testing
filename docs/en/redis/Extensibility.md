Customizing and Extending the Framework via Java
=================================================

[Russian version](../../ru/redis/Extensibility.md)

The `integration-testing-redis` module is designed as an extensible system. If your
application uses proprietary Redis clients, custom serialization protocols, or specific
database modules (e.g., RedisJSON), you can easily teach the framework to work with them.

All custom components are registered as standard Spring beans (`@Component` or `@Bean`)
and are automatically wired into the framework's data processing pipeline.

Custom Codecs and Schemas (`RedisDataCodec`, `RedisDataSchema`)
---------------------------------------------------------------

If the standard string or byte array serializers are insufficient, you can implement your
own.

### Custom Codec (`RedisDataCodec`)

A codec converts Java objects of a specific type to and from bytes. For example, a codec
for Snappy-compressed strings:

```java
package com.example.redis.codec;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import org.xerial.snappy.Snappy;

import java.nio.charset.StandardCharsets;

public class SnappyStringCodec implements RedisDataCodec {

    @Override
    public byte[] serialize(Object object) {
        if (object == null) return null;
        try {
            return Snappy.compress(object.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Data compression error", e);
        }
    }

    @Override
    public Object deserialize(byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            byte[] uncompressed = Snappy.uncompress(data);
            return new String(uncompressed, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Data decompression error", e);
        }
    }
}
```

### Custom Schema (`RedisDataSchema`)

A schema combines codecs for plain values, hash keys, and hash values. You can write your
own implementation from scratch or use the ready-made `ComposedRedisDataSchema` constructor:

```java

@Configuration
public class MyTestConfig {

    @Bean
    public RedisDataSchema snappySchema() {
        RedisDataCodec snappy = new SnappyStringCodec();
        RedisDataCodec standardString = new StringRedisDataCodec();

        // Compress values with Snappy, keep hash field keys as plain strings
        return new ComposedRedisDataSchema(snappy, standardString, snappy);
    }
}
```

Once the bean is registered in the context, you can reference it in `application-test.yml`:

```yaml
connections:
  redisConnectionFactory:
    schemas:
      "compressed:":
        bean-ref: "snappySchema"
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
import io.github.dimkich.integration.testing.redis.codec.RedisDataSchema;
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

Codec and Schema Adapters (`RedisDataCodecAdapter`, `RedisDataSchemaAdapter`)
-----------------------------------------------------------------------------

Your application may already have complex Redis clients configured (e.g., proprietary
Jedis wrappers or third-party clients) that hold serialization rules internally. To avoid
duplicating their code in tests, write an **Adapter**.

An adapter teaches the `RedisObjectFactory` factory to extract serializers from your
internal beans on the fly.

### Example: Schema Adapter for a Custom Client

Suppose your application declares a custom client bean:

```java
public class CustomRedisClient {
    private final MyCustomSerializer valueSerializer;
    private final MyCustomSerializer hashSerializer;
    // ...
}
```

Implement the `RedisDataSchemaAdapter` to convert this client into a framework-compatible
schema:

```java
package com.example.redis.adapter;

import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchemaAdapter;
import io.github.dimkich.integration.testing.redis.schema.ComposedRedisDataSchema;
import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import com.example.CustomRedisClient;
import org.springframework.stereotype.Component;

@Component // Adapter is automatically registered in the framework registry
public class CustomClientSchemaAdapter implements RedisDataSchemaAdapter {

    @Override
    public RedisDataSchema tryCreateSchema(Object bean) {
        if (bean instanceof CustomRedisClient client) {
            // Create a codec wrapper for values
            RedisDataCodec valueCodec = new RedisDataCodec() {
                @Override
                public byte[] serialize(Object obj) {
                    return client.getValueSerializer().toBytes(obj);
                }

                @Override
                public Object deserialize(byte[] bytes) {
                    return client.getValueSerializer().fromBytes(bytes);
                }
            };

            // Create a codec wrapper for hashes
            RedisDataCodec hashCodec = new RedisDataCodec() {
                @Override
                public byte[] serialize(Object obj) {
                    return client.getHashSerializer().toBytes(obj);
                }

                @Override
                public Object deserialize(byte[] bytes) {
                    return client.getHashSerializer().fromBytes(bytes);
                }
            };

            return new ComposedRedisDataSchema(valueCodec, hashCodec, hashCodec);
        }
        return null; // Pass to other adapters if the bean is not our type
    }
}
```

Now in your YAML configuration you can directly pass your `customRedisClient` bean as
a default schema or a prefix schema:

```yaml
connections:
  redisConnectionFactory:
    defaultSchema:
      bean-ref: "customRedisClientBean"  # Will be successfully converted by the adapter
```

Custom Binary Format Tags (`BinarySegmentProvider`, `BinarySegment`)
---------------------------------------------------------------------

If you need to wrap data in custom binary headers (e.g., unique hash sums, signatures,
or dynamic authorization tokens), you can add a custom tag to the `BinaryFormatParser`
syntax.

To do this, implement the segment interface `BinarySegment` and its factory
`BinarySegmentProvider`.

### Example: Creating the `{XORMASK(mask)}` Tag

**1. Create the segment (read/write byte logic):**

```java
package com.example.redis.segment;

import io.github.dimkich.integration.testing.redis.codec.segment.BinarySegment;
import io.github.dimkich.integration.testing.redis.codec.segment.ReadContext;

import java.nio.ByteBuffer;

public class XorMaskSegment implements BinarySegment {
    private final byte mask;

    public XorMaskSegment(byte mask) {
        this.mask = mask;
    }

    @Override
    public void read(ByteBuffer buffer, ReadContext ctx) {
        byte maskedValue = buffer.get();
        byte originalValue = (byte) (maskedValue ^ mask);
    }

    @Override
    public void write(ByteBuffer buffer, byte[] payload) {
        byte originalValue = 0x5A;
        buffer.put((byte) (originalValue ^ mask));
    }

    @Override
    public int length() {
        return 1;
    }
}
```

**2. Register the segment provider as `@Component`:**

```java
package com.example.redis.segment;

import io.github.dimkich.integration.testing.redis.codec.segment.BinarySegment;
import io.github.dimkich.integration.testing.redis.codec.segment.BinarySegmentProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component // Provider is automatically wired into the BinaryFormatParser
public class XorMaskSegmentProvider implements BinarySegmentProvider {

    @Override
    public String getName() {
        return "XORMASK"; // Tag name for use in YAML (case-insensitive)
    }

    @Override
    public BinarySegment create(List<String> params) {
        byte mask = (byte) Integer.decode(params.get(0)).intValue();
        return new XorMaskSegment(mask);
    }
}
```

After this, you can use the `{XORMASK}` tag in binary format settings:

```yaml
valueBinaryFormat: "{XORMASK(0xAA)}{LEN(SHORT, BE)}{CONTENT}"
```

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
