Data Preparation and Database Snapshots
========================================

[Russian version](../../ru/redis/Data-Types.md)

Testing logic with Redis always consists of two steps:

1. **Writing the Initial State (Seed):** Using the `<init>` block you populate Redis with test keys.
2. **Verifying Changes (Assert):** Using the `<diff>` block you describe which keys should
   have been added, changed, or deleted in the database after your application code ran.

Solving the Colon Problem in Redis Keys
----------------------------------------

Redis conventionally separates logical parts of a key with a colon, e.g.: `user:profile:100`.
However, in XML the colon character is reserved for namespaces (`namespace:tag`). If you try to
write a tag like `<user:profile:100>`, the XML parser will throw a syntax error.

That is why you should use the `LinkedHashMapStringObject` type for keys: the name is technical, but
what matters is that the key is passed as a plain string in the `key="..."` attribute inside the
`<entry>` tag, where any characters are allowed, including colons:

```xml
<!-- Example of correctly writing a key with a colon -->
<map type="LinkedHashMapStringObject">
    <entry key="user:profile:100">ACTIVE</entry>
</map>
```

In the `<init>` examples below the key map is written exactly like this — just put your own keys
into the `key` attribute.

Initializing the Database via `<init>`
--------------------------------------

The initialization section is described at the beginning of a container or test case. For Redis,
the `KeyValueStorageInit` initializer type is used:

```xml

<init type="KeyValueStorageInit" name="connection_name" clear="true/false">
    <map type="LinkedHashMapStringObject">
        <!-- Your data -->
    </map>
</init>
```

* `name` — the connection name from `integration.testing.redis.connections` (for example,
  `redissonConnectionFactory`).
* `clear="true"` — completely clear the database before the test. When the in-memory mirror is
  empty, the `FLUSHALL` command is not sent.
* `clear="false"` — do not clear the database (new keys will be added or existing ones updated).

The initializer supports two data formats: **Abbreviated** and **Full**.

### Abbreviated Format

Used for quickly writing simple strings, numbers, or flags when keys have no TTL.

In this format, the key value is written as text directly inside the `<entry>` tag:

```xml

<init type="KeyValueStorageInit" name="redissonConnectionFactory" clear="true">
    <map type="LinkedHashMapStringObject">
        <!-- Will be saved as a String -->
        <entry key="global:app:status">ACTIVE</entry>

        <!-- Will be saved as the string "5000" (the Integer type hints the parser) -->
        <entry key="config:timeout" type="Integer">5000</entry>

        <!-- Will be saved as the string "true" -->
        <entry key="feature:flag" type="Boolean">true</entry>

        <!-- Binary data in Base64 encoding -->
        <entry key="security:salt" type="byte[]">gQ==</entry>
    </map>
</init>
```

### Full Format

Used when you need to set a key's time-to-live (`ttl`) or write complex data structures (hashes,
lists, sets, sorted sets, streams).

In full mode, the key value is marked with `utype="RedisEntry"`, and the `<data>` (the value
itself) and `<ttl>` (the lifetime duration) tags are placed inside:

```xml

<init type="KeyValueStorageInit" name="redissonConnectionFactory" clear="true">
    <map type="LinkedHashMapStringObject">

        <!-- Example string with a 5-minute TTL -->
        <entry key="temp:otp:code" utype="RedisEntry">
            <data>9941</data>
            <ttl>PT5M</ttl> <!-- ISO-8601 format: PT5M = 5 minutes, PT30S = 30 seconds -->
        </entry>

    </map>
</init>
```

> **Tip:** If TTL is not needed, use the abbreviated format without `RedisEntry` — examples for each
> data type are shown in the sections below.

Verifying Changes via `<diff>`
------------------------------------------

The `<diff>` block describes what changes occurred in Redis after your application ran.
It is built by comparing the state "before" and "after" the test execution.

A storage record is a Redis key: the `key` attribute, or `db/key` when `multipleDatabases` is
enabled. The operation is the element tag (`insert`, `update` or `delete`). The body is the key
value: a string is written as text, a structure as fields or items, a TTL as the `ttl` attribute. A
value that did not exist is written as `insert`, a changed one as `update`, a removed one as
`delete`.

Example: the key was deleted:

```xml
<diff storage="redissonConnectionFactory">
    <delete key="user:session:100"/>
</diff>
```

Example: a string with a TTL:

```xml
<diff storage="redissonConnectionFactory">
    <insert key="user:session:100" ttl="PT30M">ACTIVE_SESSION</insert>
</diff>
```

Example: a field with its own TTL was added to a hash:

```xml
<diff storage="redissonConnectionFactory">
    <update key="user:profile:100" type="RedisHash">
        <insert key="temp_token" ttl="PT30S">token_abc_123</insert>
    </update>
</diff>
```

The TTL is an ordinary attribute of the record. Like every attribute, it describes the state after
the test: an `update` repeats the `ttl` even when the TTL did not change, and a record without the
attribute has no TTL. A TTL decrease caused by a time shift is not a change by itself: the "before"
and "after" snapshots are recalculated at the same instant.

A hash field key can be an object too: when the field key type is structured, the field name is
written as a `<key>` structure, exactly like a key of any other map; return a scalar from
`addDiffKeyPresenter` to shorten it to a line (see
[Diff Extensibility](../Storage-Diff-Extensibility.md#record-key--adddiffkeypresenter)).

XML Representation for All Redis Data Structures (Full Format)
---------------------------------------------------------------

Below are ready-to-use templates for writing any Redis data type in full format. Use them in
`<init>` (to prepare the state) — this is the value format, not the diff. `<diff>` is
built according to the rules above; the shared contract is collected in
[Storage Changes](../Storage-Diff.md).

### HASH

In Redis, a hash stores a set of fields and values. In our system it is represented by the
`RedisHash` type. Each hash field is an independent `RedisEntry`, so each field can have its
own TTL (via the `HPEXPIREAT` command, Redis 7.4+).

```xml
<entry key="user:profile:100" utype="RedisEntry">
    <data type="RedisHash">
        <!-- Field without TTL (permanent) -->
        <entry>
            <key>username</key>
            <value type="RedisEntry">
                <data>Ivan</data>
            </value>
        </entry>
        <!-- Field with an individual TTL of 30 seconds -->
        <entry>
            <key>temp_token</key>
            <value type="RedisEntry">
                <data>token_abc_123</data>
                <ttl>PT30S</ttl>
            </value>
        </entry>
    </data>
</entry>
```

**Abbreviated format (no TTL):**

```xml
<entry key="user:profile:100" utype="RedisHash">
    <entry>
        <key>username</key>
        <value>
            <data>Ivan</data>
        </value>
    </entry>
    <entry>
        <key>role</key>
        <value>
            <data>admin</data>
        </value>
    </entry>
</entry>
```

### LIST

Lists preserve strict insertion order. They are represented by the `RedisList` type.

```xml
<entry key="notifications:queue" utype="RedisEntry">
    <data type="RedisList">
        <value>first_message</value>
        <value>second_message</value>
    </data>
</entry>
```

**Abbreviated format (no TTL):**

```xml
<entry key="notifications:queue" utype="RedisList">
    <value>first_message</value>
    <value>second_message</value>
</entry>
```

### SET

Sets contain only unique elements. Elements are sorted by `RedisDataComparator`: strings
lexicographically, numbers numerically. Represented by the `RedisSet` type.

```xml
<entry key="user:roles:1" utype="RedisEntry">
    <data type="RedisSet">
        <value>ROLE_ADMIN</value>
        <value>ROLE_USER</value>
    </data>
</entry>
```

**Abbreviated format (no TTL):**

```xml
<entry key="user:roles:1" utype="RedisSet">
    <value>ROLE_ADMIN</value>
    <value>ROLE_USER</value>
</entry>
```

### ZSET (Sorted Set)

Each entry consists of a member and its numeric score, which determines sort order. Represented
by the `RedisZSet` type, with elements of type `RedisZSetEntry`.

```xml
<entry key="leaderboard" utype="RedisEntry">
    <data type="RedisZSet">
        <value type="RedisZSetEntry">
            <member>player_A</member>
            <score type="BigDecimal">1500</score>
        </value>
        <value type="RedisZSetEntry">
            <member>player_B</member>
            <score type="BigDecimal">2100.50</score>
        </value>
    </data>
</entry>
```

**Abbreviated format (no TTL):**

```xml
<entry key="leaderboard" utype="RedisZSet">
    <value type="RedisZSetEntry">
        <member>player_A</member>
        <score type="BigDecimal">1500</score>
    </value>
    <value type="RedisZSetEntry">
        <member>player_B</member>
        <score type="BigDecimal">2100.50</score>
    </value>
</entry>
```

**In a diff** a sorted set is written by members: a member identifies the element, so it becomes the
`key`, and its score is the `score` attribute. A member that appears is an `insert`, a changed score
is an `update`, a removed member is a `delete`:

```xml
<insert key="leaderboard" type="RedisZSet">
    <player_A score="1500"/>
    <player_C score="1200"/>
</insert>
```

A score change keeps the member in place, so a sorted set update is one line rather than a new
element. This requires a member to be expressible as an identifier: a string is defined by
[`addDiffKeyPresenter`](../Storage-Diff-Extensibility.md#record-key--adddiffkeypresenter) returning a scalar,
while a structured object is written as an object key by default, and its projection can be
refined by the same method. The member is then written as a structure, while the score stays an
attribute:

```xml
<insert key="leaderboard" type="RedisZSet">
    <insert score="1500">
        <key type="MarketInstrumentKey">
            <exchange>MOEX</exchange>
            <ticker>SBER</ticker>
        </key>
    </insert>
</insert>
```

If members are large values without an identifier, the sorted set is better presented as a
collection: register a presenter for `RedisZSet` that returns the set itself and a presenter for
`RedisZSetEntry` whose body is the member and whose attribute is the score — then the framework reads
the value as an ordinary collection and writes `<insert score="1500">{big member}</insert>`.

### STREAM

Streams store an ordered sequence of records, each with a unique ID (e.g., `1716530000000-0`)
and a set of fields. Represented by the `RedisStream` type, with elements of type
`RedisStreamEntry`.

```xml

<entry key="orders:stream" utype="RedisEntry">
    <data type="RedisStream">
        <value type="RedisStreamEntry" id="1716530000000-0">
            <fields>
                <key>order_id</key>
                <value>ORD-99</value>
                <key>amount</key>
                <value>450.00</value>
            </fields>
        </value>
    </data>
</entry>
```

**Abbreviated format (no TTL):**

```xml
<entry key="orders:stream" utype="RedisStream">
    <value type="RedisStreamEntry" id="1716530000000-0">
        <fields>
            <key>order_id</key>
            <value>ORD-99</value>
            <key>amount</key>
            <value>450.00</value>
        </fields>
    </value>
</entry>
```

### HyperLogLog (HLL)

Since HyperLogLog is used for probabilistic unique element counting, the framework stores it
as a regular unique set for testing convenience. Represented by the `RedisHyperLogLog` type.

```xml
<entry key="unique:visitors" utype="RedisEntry">
    <data type="RedisHyperLogLog">
        <value>user_ip_1</value>
        <value>user_ip_2</value>
    </data>
</entry>
```

**Abbreviated format (no TTL):**

```xml

<entry key="unique:visitors" utype="RedisHyperLogLog">
    <value>user_ip_1</value>
    <value>user_ip_2</value>
</entry>
```

---
[← Back to Home](../README.md)
