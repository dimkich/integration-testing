Redis Module Configuration: Parameter Reference
================================================

[Russian version](../../ru/redis/Configuration.md)

All Redis testing parameters are defined in standard Spring Boot configuration files (such as
`application.yml` or `application-test.yml`) under the `integration.testing.redis` prefix.

The settings allow you to declaratively link configuration files with Spring connection beans
in your project, automatically inherit common parameters, and resolve serialization rules
without conflicts.

Linking Configuration to Spring Connection Beans
------------------------------------------------

The key section is the `connections` block. The subsection names in this section must exactly
match the `RedisConnectionFactory` bean names declared in your application's Java code.

If your project declares two connection factories (for example, one for Jedis and one for
Redisson):

```java

@Configuration
public class MyRedisAppConfig {

    @Bean
    public RedisConnectionFactory redissonConnectionFactory() {
        return new RedissonConnectionFactory(...);
    }

    @Bean
    public RedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory(...);
    }
}
```

Then in `application-test.yml` you describe the connections using the same bean names as keys
in the `connections` block:

```yaml
integration:
  testing:
    redis:
      connections:
        # The key name matches the @Bean method name for Redisson
        redissonConnectionFactory:
          host: "localhost"
          port: 6379

        # The key name matches the @Bean method name for Jedis
        jedisConnectionFactory:
          host: "localhost"
          port: 6380
```

If your application uses standard Spring Boot auto-configuration and has only one database,
the default bean name is `redisConnectionFactory`.

Inheritance and Deep Merge Pipeline
-----------------------------------

Parameters are not simply copied; they go through a three-level deep merge process from top
to bottom:

```
[ Level 1: Global Settings ] (set directly in `integration.testing.redis.*`)
                 │
                 ▼
  [ Level 2: Connection Settings ] (override globals for a specific bean)
                 │
                 ▼
  [ Level 3: Key Schema Settings ] (define rules for a specific key mask/prefix)
```

### Parameter Merge Rules

**Simple values (String, Integer, Boolean, Long):**

* If a child object's (e.g., a specific schema) field value is `null`, it is replaced by the
  inherited parent value.
* If the child object's field is set, it is kept and the parent value is ignored.

**Lists and Sets (`Set<String>` excludedFields):**

* A union operation is performed. All unique elements from parent lists are added to the child.
* Example: If at global Level 1 you excluded `createdAt`, at Connection Level 2 you added
  `updatedAt`, and at Level 3 schema you added `hits` — the framework will automatically
  exclude all three fields: `createdAt`, `updatedAt`, and `hits`.

**Exclusive Property Groups (`@PropertyInheritanceExclusive` annotation):**

* Fields that define how serializers are specified — the Spring bean name (`beanRef`) and
  the fully qualified Java class name (`classRef`) — are grouped into an exclusive group `"ref"`.
* Group rule: If a child configuration has at least one field from the exclusive group set,
  then none of the fields from that group will be inherited from the parent. This prevents
  conflicts (e.g., when a child wants to use a local `classRef` but also inherits a parent's
  `beanRef`).

Complete Parameter Reference
----------------------------

### Global Level Parameters (`integration.testing.redis.*`)

These settings apply to all connections by default.

| Parameter              | Type                      | Description                                                            | Default                      |
|------------------------|---------------------------|------------------------------------------------------------------------|------------------------------|
| `host`                 | String                    | Host for connecting to Redis (the replicator connects here)            | `${embedded.redis.host}`     |
| `port`                 | Integer                   | Port for connecting to Redis                                           | `${embedded.redis.port}`     |
| `user`                 | String                    | Username for Redis authentication (root is treated as null)            | `${embedded.redis.user}`     |
| `password`             | String                    | Password for authentication                                            | `${embedded.redis.password}` |
| `multipleDatabases`    | Boolean                   | Enable key separation by database index (`SELECT`)                     | `false`                      |
| `syncBarrierTimeoutMs` | Long                      | Maximum wait time for the replication sync barrier in milliseconds     | `5000`                       |
| `keyCodec`             | Codec                     | Global key codec configuration                                         | `StringRedisDataCodec`       |
| `defaultSchema`        | Schema                    | Global default serialization schema                                    | `StringRedisDataSchema`      |
| `excludedFields`       | Set\<String\>             | Global list of fields to exclude from comparison in DTOs/maps          | Empty                        |
| `connections`          | Map\<String, Connection\> | Map of Redis connections, keys are `RedisConnectionFactory` bean names | Empty                        |

### Codec Object Structure (`keyCodec` or `Codec`)

Used for configuring key encoding rules.

| Parameter           | Type          | Description                                                                         | Exclusivity   |
|---------------------|---------------|-------------------------------------------------------------------------------------|---------------|
| `beanRef`           | String        | Spring bean name of a `RedisDataCodec`                                              | Group `"ref"` |
| `classRef`          | String        | Fully qualified class name of a `RedisDataCodec` to create via reflection           | Group `"ref"` |
| `excludedFields`    | Set\<String\> | List of fields to exclude within the key structure (if the key is a complex object) | —             |
| `valueBinaryFormat` | String        | Binary envelope template for keys                                                   | —             |

### Schema Object Structure (`defaultSchema`, `Schema`)

Used for configuring value serialization rules (plain values, collection elements, or hash
fields).

| Parameter               | Type          | Description                                                                    | Exclusivity   |
|-------------------------|---------------|--------------------------------------------------------------------------------|---------------|
| `beanRef`               | String        | Spring bean name of a `RedisDataSchema` or integration class (`RedisTemplate`) | Group `"ref"` |
| `classRef`              | String        | Fully qualified class name of a `RedisDataSchema` to create via reflection     | Group `"ref"` |
| `ignore`                | Boolean       | `true` — completely ignore keys matching this schema (hide from comparisons)   | —             |
| `excludedFields`        | Set\<String\> | List of value fields to exclude from comparison                                | —             |
| `valueBinaryFormat`     | String        | Binary envelope template for plain values and list/set elements                | —             |
| `hashKeyBinaryFormat`   | String        | Binary envelope template for hash field keys                                   | —             |
| `hashValueBinaryFormat` | String        | Binary envelope template for hash field values                                 | —             |

### Connection Level Parameters (`connections.<ConnectionFactoryName>.*`)

Describe settings for a specific `RedisConnectionFactory`. You can override any Global Level
parameters here:

| Parameter           | Type                                                                     | Description                                                                  | Inheritance                                      |
|---------------------|--------------------------------------------------------------------------|------------------------------------------------------------------------------|--------------------------------------------------|
| `host`              | String                                                                   | Host for connecting to Redis (the replicator connects here)                  | Overrides global `host`                          |
| `port`              | Integer                                                                  | Port for connecting to Redis                                                 | Overrides global `port`                          |
| `user`              | String                                                                   | Username for Redis authentication (root is treated as null)                  | Overrides global `user`                          |
| `password`          | String                                                                   | Password for authentication                                                  | Overrides global `password`                      |
| `multipleDatabases` | Boolean                                                                  | Enable key separation by database index (`SELECT`)                           | Overrides global `multipleDatabases`             |
| `keyCodec`          | [`Codec`](#codec-object-structure-keycodec-or-codec)                     | Key codec configuration for this connection                                  | Fully replaces global `keyCodec`                 |
| `defaultSchema`     | [`Schema`](#schema-object-structure-defaultschema-schema)                | Default serialization schema for this factory                                | Fully replaces global `defaultSchema`            |
| `excludedFields`    | Set\<String\>                                                            | List of fields to exclude from comparison in DTOs/maps                       | Merged (Union) with global list                  |
| `schemas`           | Map\<String, [`Schema`](#schema-object-structure-defaultschema-schema)\> | Map of individual schemas for key groups (Level 3). Keys are prefix patterns | Not inherited (defined only at connection level) |

Key Pattern Resolution (Longest-Prefix Match)
---------------------------------------------

When any key is received from Redis, the framework must determine which schema to use. The
`ConnectionSchemaRegistry` performs a trie-based search:

1. The key is converted to a string representation using the key codec.
2. The framework looks for the longest matching prefix among the keys in the `schemas` section.
3. If a match is found, that schema's configuration is applied.
4. If no match is found, the connection's default schema (`defaultSchema`) is used.

### Search Example

Suppose the following schema configuration is set:

```yaml
schemas:
  "redisson.TestRedisDto": # Schema A
    bean-ref: testRedisTemplate
  "redisson.TestRedisDto.backup": # Schema B
    ignore: true
```

| Key                                | Longest matching prefix        | Result                            |
|------------------------------------|--------------------------------|-----------------------------------|
| `redisson.TestRedisDto:123`        | `redisson.TestRedisDto`        | Schema A is applied               |
| `redisson.TestRedisDto.backup:999` | `redisson.TestRedisDto.backup` | Schema B is applied (key ignored) |
| `redisson.otherKey`                | No match                       | `defaultSchema` is applied        |

### Escaping Special Characters in YAML

If your keys contain dots, square brackets, or other special characters, wrap the prefixes
in quotes and square brackets so the YAML parser recognizes them correctly:

```yaml
schemas:
  "[redisson.bit]": # Required quotes and brackets when dots are present
    class-ref: io.github.dimkich.integration.testing.redis.schema.ByteArrayRedisDataSchema
```

Ignoring Technical Noise (`ignore: true`)
-----------------------------------------

During a test, background application processes often write technical information to Redis
(service logs, background task sessions, lock keys). These changes clutter the `dataStorageDiff`
comparison block and can cause test failures due to unpredictable behavior.

You can tell the framework to completely ignore such keys using the `ignore: true` parameter:

```yaml
schemas:
  # Completely ignore all Redisson Lock keys
  "redisson_lock:":
    ignore: true
```

Ignored keys are automatically filtered out and will not appear in comparison reports or
`dataStorageDiff` blocks.

Complete Configuration Example (application-test.yml)
------------------------------------------------------

Below is an example of a test environment setup with two Redis connections and hierarchical
serialization overrides:

```yaml
integration:
  testing:
    redis:
      # --- LEVEL 1: Global settings ---
      host: ${embedded.redis.host}
      port: ${embedded.redis.port}
      password: ${embedded.redis.password}
      syncBarrierTimeoutMs: 10000   # Wait for replication confirmation up to 10 seconds

      # Exclude technical dates from all DTOs by default
      excludedFields:
        - "createdAt"
        - "updatedAt"

      # All data is treated as plain strings by default
      defaultSchema:
        class-ref: io.github.dimkich.integration.testing.redis.schema.StringRedisDataSchema

      connections:
        # --- LEVEL 2: "redissonConnectionFactory" connection ---
        redissonConnectionFactory:
          defaultSchema:
            # Override the default schema to RedissonClient
            bean-ref: "redissonClient"
          schemas:
            # --- LEVEL 3: Schemas for this connection ---
            "[redisson.TestRedisDto]":
              bean-ref: "testRedisTemplate"
              excludedFields:
                - "metrics.hits"   # Additionally exclude the hit counter inside this DTO
            "[redisson.bit]":
              class-ref: "io.github.dimkich.integration.testing.redis.schema.ByteArrayRedisDataSchema"
            "redisson_lock:":
              ignore: true

        # --- LEVEL 2: "jedisConnectionFactory" connection ---
        jedisConnectionFactory:
          multipleDatabases: true
          defaultSchema:
            excludedFields:
              - "version"           # Additionally exclude version for this connection
          schemas:
            redisson:
              ignore: true
```

---
[← Back to Home](../README.md)
