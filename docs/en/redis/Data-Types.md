Data Preparation and Database Snapshots
========================================

[Russian version](../../ru/redis/Data-Types.md)

Testing logic with Redis always consists of two steps:

1. **Writing the Initial State (Seed):** Using the `<init>` block you populate Redis with test keys.
2. **Verifying Changes (Assert):** Using the `<dataStorageDiff>` block you describe which keys should
   have been added, changed, or deleted in the database after your application code ran.

Solving the Colon Problem in Redis Keys
----------------------------------------

Redis conventionally separates logical parts of a key with a colon, e.g.: `user:profile:100`.
However, in XML the colon character is reserved for namespaces (`namespace:tag`). If you try to
write a tag like `<user:profile:100>`, the XML parser will throw a syntax error.

To avoid this, the framework uses a special data type `LinkedHashMapStringObject`. It allows you
to pass the key name as a plain string in the `key="..."` attribute inside the `<entry>` tag.
This enables you to use absolutely any characters in keys, including colons:

```xml
<!-- Example of correctly writing a key with a colon -->
<map type="LinkedHashMapStringObject">
    <entry key="user:profile:100">ACTIVE</entry>
</map>
```

Initializing the Database via `<init>`
--------------------------------------

The initialization section is described at the beginning of a container or test case. For Redis,
the `keyValueStorageInit` initializer type is used:

```xml

<init type="keyValueStorageInit" name="connection_name" clear="true/false">
    <map type="LinkedHashMapStringObject">
        <!-- Your data -->
    </map>
</init>
```

* `name` — the connection name (matches the `RedisConnectionFactory` bean name in your project,
  e.g., `redissonConnectionFactory`).
* `clear="true"` — completely clear the database before the test (executes `FLUSHALL` on the server).
* `clear="false"` — do not clear the database (new keys will be added or existing ones updated).

The initializer supports two data formats: **Abbreviated** and **Full**.

### Abbreviated Format

Used for quickly writing simple strings, numbers, or flags when keys have no TTL.

In this format, the key value is written as text directly inside the `<entry>` tag:

```xml

<init type="keyValueStorageInit" name="redissonConnectionFactory" clear="true">
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

In full mode, the key value must be wrapped in a `<value type="RedisEntry">` tag containing
`<data>` (the value itself) and `<ttl>` (the lifetime duration):

```xml

<init type="keyValueStorageInit" name="redissonConnectionFactory" clear="true">
    <map type="LinkedHashMapStringObject">

        <!-- Example string with a 5-minute TTL -->
        <entry key="temp:otp:code" type="RedisEntry">
            <data>9941</data>
            <ttl>PT5M</ttl> <!-- ISO-8601 format: PT5M = 5 minutes, PT30S = 30 seconds -->
        </entry>

    </map>
</init>
```

> **Tip:** If TTL is not needed, use the abbreviated format with `utype` — examples for each
> data type are shown in the sections below.

Verifying Changes via `<dataStorageDiff>`
------------------------------------------

The `<dataStorageDiff>` block describes what changes occurred in Redis after your application ran.
It is built by comparing the state "before" and "after" the test execution.

Each entry inside the diff has a `change` attribute that indicates the type of change:

* `change="added"` — a key or element was added to the empty database.
* `change="changed"` — an existing key or element value was modified.
* `change="deleted"` — a key or element was completely removed.

Example diff for a deleted key:

```xml
<dataStorageDiff type="MapStringKeyObjectValue">
    <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
        <!-- Indicate that the key was deleted -->
        <entry change="deleted">
            <key>user:session:100</key>
            <!-- Always pass an empty nil value for a deleted element -->
            <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
        </entry>
    </redissonConnectionFactory>
</dataStorageDiff>
```

XML Representation for All Redis Data Structures (Full Format)
---------------------------------------------------------------

Below are ready-to-use templates for writing and comparing all Redis data types in full format.
You can use them both in `<init>` (for preparation) and in `<dataStorageDiff>` (for verification).

### HASH

In Redis, a hash stores a set of fields and values. In our system it is represented by the
`RedisHash` type. Each hash field is an independent `RedisEntry`, so each field can have its
own TTL (via the `HEXPIRE` command).

```xml
<entry key="user:profile:100" type="RedisEntry">
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
<entry key="notifications:queue" type="RedisEntry">
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

Sets contain only unique elements. Elements in XML are automatically sorted alphabetically for
stable test comparison. Represented by the `RedisSet` type.

```xml
<entry key="user:roles:1" type="RedisEntry">
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
<entry key="leaderboard" type="RedisEntry">
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

### STREAM

Streams store an ordered sequence of records, each with a unique ID (e.g., `1716530000000-0`)
and a set of fields. Represented by the `RedisStream` type, with elements of type
`RedisStreamEntry`.

```xml

<entry key="orders:stream" type="RedisEntry">
    <data type="RedisStream">
        <value type="RedisStreamEntry" id="1716530000000-0">
            <fields>
                <entry>
                    <key>order_id</key>
                    <value>ORD-99</value>
                </entry>
                <entry>
                    <key>amount</key>
                    <value>450.00</value>
                </entry>
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
            <entry>
                <key>order_id</key>
                <value>ORD-99</value>
            </entry>
            <entry>
                <key>amount</key>
                <value>450.00</value>
            </entry>
        </fields>
    </value>
</entry>
```

### HyperLogLog (HLL)

Since HyperLogLog is used for probabilistic unique element counting, the framework stores it
as a regular unique set for testing convenience. Represented by the `RedisHyperLogLog` type.

```xml
<entry key="unique:visitors" type="RedisEntry">
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
