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
        return new RedissonConnectionFactory(...)
    }

    @Bean
    public RedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory(...);
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

If your application uses standard Spring Boot automatic configuration and has only one database,
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

**Mutually Exclusive Fields (`bean-ref` and `type`):**

* Inside the record base, each schema component and the key codec (`keyCodec`), `beanRef` and `type`
  are two mutually exclusive ways to select the source: only one of them can be set.
* If a child configuration sets at least one of these fields, the other one is not inherited from
  the parent. This prevents a conflict where a child component defines `bean-ref` but also inherits
  the parent's `type`.

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
| `keyCodec`             | Serde config              | Global key codec configuration                                         | `string`                     |
| `defaultSchema`        | Schema                    | Global default serialization schema                                    | all components — `string`    |
| `excludedFields`       | Set\<String\>             | Global list of fields to exclude from comparison in DTOs/maps          | Empty                        |
| `connections`          | Map\<String, Connection\> | Map of Redis connections, keys are `RedisConnectionFactory` bean names | Empty                        |

### Key Codec Structure (`keyCodec`)

The key codec is a single-slot source: a core serde provider (`string`, `json`, ...), a
Spring Data `RedisSerializer` or a Redisson `Codec`/`RedissonClient`. The
common fields and source selection rules are defined by the serde core config
(`io.github.dimkich.integration.testing.serde.StandardSerdeProperties`).

| Parameter        | Type   | Description                                                                | Exclusivity   |
|------------------|--------|----------------------------------------------------------------------------|---------------|
| `type`           | String | Core provider name (`string`, `json`, ...) or source fully qualified class | Group `"ref"` |
| `beanRef`        | String | Spring bean name of the source                                             | Group `"ref"` |
| `binaryEnvelope` | String | Binary envelope template for keys                                          | —             |

### Schema Structure (`defaultSchema`, `schemas.*`)

A schema is described by components — `value`, `hash-key`, `hash-value`, each with its own
single-slot source (a core provider, a Spring Data `RedisSerializer` or a Redisson
`Codec`/`RedissonClient`). Unset components are inherited from the parent schema by the property
merge; a component may set only `binary-envelope` — the source comes from the parent schema.

| Parameter           | Type          | Description                                                                                          | Exclusivity   |
|---------------------|---------------|------------------------------------------------------------------------------------------------------|---------------|
| `type`              | String        | Core provider name (`string`, `json`, ...) or source fully qualified class — base for all components | Group `"ref"` |
| `bean-ref`          | String        | Spring bean name of the source — base for all components                                             | Group `"ref"` |
| `target-class`      | String        | Target value class for `json`/`xml`/`yaml` providers — base for all components                       | —             |
| `object-mapper-ref` | String        | `ObjectMapper` bean name for `json`/`xml`/`yaml` providers — base for all components                 | —             |
| `value`             | Serde config  | Codec for plain values and list/set/stream elements                                                  | —             |
| `hash-key`          | Serde config  | Codec for hash field keys                                                                            | —             |
| `hash-value`        | Serde config  | Codec for hash field values                                                                          | —             |
| `ignore`            | Boolean       | `true` — completely ignore keys matching this schema (hide from comparisons)                         | —             |
| `excludedFields`    | Set\<String\> | List of value fields to exclude from comparison                                                      | —             |

### Record Base

The `type`, `bean-ref`, `target-class` and `object-mapper-ref` fields can be set directly on the
schema level — this is the base for all components: when the configuration is loaded they are
merged into `value`, `hash-key` and `hash-value`, and a value set on a component overrides the
base. If the base does not define a source, components use `string`. `binary-envelope` is not
supported in the base: the envelope is set on each component separately.

```yaml
defaultSchema:
  type: json
  value:
    type: bytes   # the component overrides the base
```

The base is inherited along the same chain (global schema → connection schema → prefix schema),
and `bean-ref` and `type` remain mutually exclusive: a child schema source blocks the inheritance
of the parent source.

Each component is a regular serde core config
(`io.github.dimkich.integration.testing.serde.StandardSerdeProperties`):

| Parameter         | Type   | Description                                                                | Exclusivity   |
|-------------------|--------|----------------------------------------------------------------------------|---------------|
| `type`            | String | Core provider name (`string`, `json`, ...) or source fully qualified class | Group `"ref"` |
| `beanRef`         | String | Spring bean name of the source                                             | Group `"ref"` |
| `targetClass`     | String | Target value class for `json`/`xml`/`yaml` providers                       | —             |
| `objectMapperRef` | String | `ObjectMapper` bean name for `json`/`xml`/`yaml` providers                 | —             |
| `binaryEnvelope`  | String | Binary envelope template for this component                                | —             |

`target-class` and `object-mapper-ref` can be set both in the record base and on a component (the
component overrides); `binary-envelope` belongs to components only.

Example: each component has its own source and envelope:

```yaml
schemas:
  "redisson.value:":
    value:
      bean-ref: testValueSerializer
      binary-envelope: "{FIX(8, 0)}{LEN(LONG, LE)}{CONTENT}"
    hash-key:
      type: org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
    hash-value:
      type: org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
```

The binary envelope format is described as a template of segments — see
[Binary Envelopes](../serde/Binary-Envelopes.md).

When the source is a Redisson `Codec` (including via `RedissonClient`), the core passes the slot
role to the component: `hash-key` is encoded with the `MapKeyEncoder`/`MapKeyDecoder` pair,
`hash-value` with `MapValueEncoder`/`MapValueDecoder`, and `value` with
`ValueEncoder`/`ValueDecoder`. The same bean can be referenced from all three components — see
[Redisson codec slots](Extensibility.md#redisson-codec-slots).

### Schema Component Inheritance

A schema may configure only some components: the missing ones are inherited from the parent schema
by the property merge. Hierarchy: global `defaultSchema` → connection `defaultSchema` →
`schemas.<prefix>`. By default, every component is the core `string` provider:

```yaml
integration:
  testing:
    redis:
      connections:
        redissonConnectionFactory:
          schemas:
            "user:":
              value:
                type: json        # values are JSON, hash-key/hash-value come from defaultSchema
```

The record base and components are inherited independently of each other, and inside the base and a
component `bean-ref` and `type` are exclusive alternatives: if a child base or component defines its
own source, the parent's source is not mixed in. `binary-envelope` is an ordinary component field: it
can be set without a source, and the envelope is then applied to the inherited source.
`excludedFields` are unioned.

### Connection Level Parameters (`connections.<ConnectionFactoryName>.*`)

Describe settings for a specific `RedisConnectionFactory`. You can override any Global Level
parameters here:

| Parameter           | Type                                                               | Description                                                                  | Inheritance                                                                |
|---------------------|--------------------------------------------------------------------|------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| `host`              | String                                                             | Host for connecting to Redis (the replicator connects here)                  | Overrides global `host`                                                    |
| `port`              | Integer                                                            | Port for connecting to Redis                                                 | Overrides global `port`                                                    |
| `user`              | String                                                             | Username for Redis authentication (root is treated as null)                  | Overrides global `user`                                                    |
| `password`          | String                                                             | Password for authentication                                                  | Overrides global `password`                                                |
| `multipleDatabases` | Boolean                                                            | Enable key separation by database index (`SELECT`)                           | Overrides global `multipleDatabases`                                       |
| `keyCodec`          | Serde config                                                       | Key codec configuration for this connection                                  | Inherits global `keyCodec`; fields are overridden                          |
| `defaultSchema`     | [`Schema`](#schema-structure-defaultschema-schemas)                | Default serialization schema for this factory                                | Inherits global `defaultSchema`; components can be overridden individually |
| `excludedFields`    | Set\<String\>                                                      | List of fields to exclude from comparison in DTOs/maps                       | Merged (Union) with global list                                            |
| `schemas`           | Map\<String, [`Schema`](#schema-structure-defaultschema-schemas)\> | Map of individual schemas for key groups (Level 3). Keys are prefix patterns | Not inherited (defined only at connection level)                           |

Key Pattern Resolution (Longest-Prefix Match)
---------------------------------------------

When any key is received from Redis, the framework must determine which schema to use. The rule is
simple: the most precise (longest) setting wins.

1. The key is converted to a string representation using the key codec.
2. The framework looks for the longest matching prefix among the keys in the `schemas` section: the
   more precise the key pattern, the higher its priority.
3. If a match is found, that schema's configuration is applied.
4. If no match is found, the connection's default schema (`defaultSchema`) is used.

### Search Example

Suppose the following schema configuration is set:

```yaml
schemas:
  "redisson.TestRedisDto": # Schema A
    value:
      bean-ref: testValueSerializer
  "redisson.TestRedisDto.backup": # Schema B
    ignore: true
```

| Key                                | Longest matching prefix        | Result                            |
|------------------------------------|--------------------------------|-----------------------------------|
| `redisson.TestRedisDto:123`        | `redisson.TestRedisDto`        | Schema A is applied               |
| `redisson.TestRedisDto.backup:999` | `redisson.TestRedisDto.backup` | Schema B is applied (key ignored) |
| `redisson.otherKey`                | No match                       | `defaultSchema` is applied        |

### Pub/Sub Channels

Pub/Sub channels are resolved by the same rules: the channel name is matched against the same
`schemas` prefixes with the same fallback to `defaultSchema`, and the matched `value` codec is
used to (de)serialize the payload. The channel name itself is encoded and decoded with the
connection's `keyCodec`, like key names. This is how pushes are captured and sent; see
[Push-Messages.md](Push-Messages.md) for details.

Replication is awaited through the wait-completion subsystem: after initialization (so data
storage snapshots and diffs are read from a caught-up mirror) and after the test action (so
pushes and mutations are captured before assertions). This requires
`integration.testing.wait.completion.enabled=true`; `redis.properties` already sets it, so no
extra test properties are needed. With an explicit `false` the module does not await replication.

### Escaping Special Characters in YAML

If your keys contain dots, square brackets, or other special characters, wrap the prefixes
in quotes and square brackets so the YAML parser recognizes them correctly:

```yaml
schemas:
  "[redisson.bit]": # Required quotes and brackets when dots are present
    value:
      type: bytes
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

      # All components use the core `string` provider by default (this is the default)
      defaultSchema:
        value:
          type: string
        hash-key:
          type: string
        hash-value:
          type: string

      connections:
        # --- LEVEL 2: "redissonConnectionFactory" connection ---
        redissonConnectionFactory:
          defaultSchema:
            # Override the connection components (e.g. to Jdk serialization)
            value:
              type: "org.springframework.data.redis.serializer.JdkSerializationRedisSerializer"
            hash-key:
              type: "org.springframework.data.redis.serializer.JdkSerializationRedisSerializer"
            hash-value:
              type: "org.springframework.data.redis.serializer.JdkSerializationRedisSerializer"
          schemas:
            # --- LEVEL 3: Schemas for this connection ---
            "[redisson.TestRedisDto]":
              value:
                bean-ref: "testValueSerializer"
              excludedFields:
                - "metrics.hits"   # Additionally exclude the hit counter inside this DTO
            "[redisson.bit]":
              value:
                type: bytes
              hash-key:
                type: bytes
              hash-value:
                type: bytes
            "[redisson.envelope]":
              # Components: each part has its own source and format
              value:
                bean-ref: "testValueSerializer"
                binary-envelope: "{VER(1)}{LEN(INT, BE)}{CONTENT}"
              hash-key:
                type: "org.springframework.data.redis.serializer.JdkSerializationRedisSerializer"
              hash-value:
                type: "org.springframework.data.redis.serializer.JdkSerializationRedisSerializer"
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
