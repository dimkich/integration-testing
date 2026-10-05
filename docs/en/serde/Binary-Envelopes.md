Working with Binary Envelopes
=============================

[Russian version](../../ru/serde/Binary-Envelopes.md)

In real projects, data is often not stored in plain form (e.g., JSON or a plain string). Instead, it
is wrapped in internal binary envelopes by client libraries or custom protocols.

Typical examples:

* Redisson automatically prepends a service header (codec marker, timestamps, or length) to
  your value.
* Custom protocols may require a signature (magic bytes), protocol version, payload length
  prefix at the start, and a checksum (CRC32) at the end.

The framework solves this declaratively using a template parser. You can describe the binary
envelope structure directly in the configuration using a simple text template in the
`binary-envelope` field.

Common case: Redisson
---------------------

By default, many Redisson codecs append 8 empty filler bytes and an 8-byte Little-Endian length to
the value. The template for such a value:

```yaml
value:
  bean-ref: "redissonClient"
  binary-envelope: "{FIX(8, 0)}{LEN(LONG, LE)}{CONTENT}"
```

The full example with a key codec is in [Examples](#practical-configuration-examples) below.

Which envelope do I need? Ask the developers
--------------------------------------------

If you are testing a binary protocol, ask the development team:

1. **Byte order** — Big Endian (network order, most significant byte first) or Little Endian? If
   they do not know, start with `BE`.
2. **Length prefix size** — `INT` (4 bytes) or `SHORT` (2 bytes)?
3. **Is there a signature (magic bytes)** at the start of the packet, and what is it?
4. **Where are the version, timestamp and checksum** (if any)?

The answers form the template. For example: signature `AA`, version `05`, length `SHORT` BE, CRC32
at the end — that is `AA{VER(5)}{LEN(SHORT, BE)}{CONTENT}{CRC32}`.

Where the envelope is set
-------------------------

The envelope can be combined with any single-slot source: a core provider (`type: json`) or a native
serializer. The framework converts the source to the required form and wraps it.

`binary-envelope` is set **next to the source** in the platform's serde config:

* **Redis** — on a schema component (`value`/`hash-key`/`hash-value`) or on the key codec. The
  component source may be inherited from the parent schema by the property merge, so a component
  may set only `binary-envelope`. The schema itself cannot carry a format: the envelope wraps a
  single slot, not a set of components.
* **Kafka** — on the `key`/`value` record parts. Any source that resolves to a core
  serializer works: a core provider (`type: json`) or a native Kafka `Serializer`/`Deserializer`
  (FQCN `type` or `bean-ref`). `binary-envelope` is not supported for `headers`: headers are
  serialized per value.

Binary Envelope Structure
--------------------------

A binary envelope consists of an ordered list of segments. The entire chain is divided into
three logical parts relative to the payload:

```
┌─────────────────────────────────┬───────────┬─────────────────────────────────┐
│         Header                  │  Payload   │          Footer                │
├──────────────┬──────────┬───────┼───────────┼─────────┬───────────┬───────────┤
│ {VER(byte)}  │  {FIX}   │ {LEN} │ {CONTENT} │  {TS}   │  {FIX}    │  {CRC32}  │
└──────────────┴──────────┴───────┴───────────┴─────────┴───────────┴───────────┘
```

* **Header:** All segments declared before the `{CONTENT}` tag. They are processed first
  during reading.
* **Payload (`{CONTENT}`):** Your main serialized value (e.g., the JSON of a DTO). Exactly one
  such segment must exist in the template.
* **Footer:** All segments declared after the `{CONTENT}` tag.

Template Syntax
---------------

A template is a string containing hexadecimal characters and control tags in curly braces `{}`.

### Plain Bytes (Static Hex Characters)

Any characters outside curly braces are treated as hexadecimal bytes. They write fixed markers.

* **Example:** `524453` writes 3 bytes: `0x52 0x44 0x53` (ASCII string "RDS"). During
  reading, these bytes are verified: a mismatch throws an error.

### Byte Order and Data Types for Parameters

When configuring numeric tags, standard data types and byte orders are used.

**Data Types:**

| Type              | Size    | Description           |
|-------------------|---------|-----------------------|
| `LONG`            | 8 bytes | 64-bit integer        |
| `INT` / `INTEGER` | 4 bytes | 32-bit integer        |
| `SHORT`           | 2 bytes | 16-bit integer        |
| `BYTE`            | 1 byte  | 8-bit integer         |
| `DOUBLE`          | 8 bytes | Floating point number |
| `FLOAT`           | 4 bytes | Floating point number |

When read, `BYTE` and `SHORT` are treated as unsigned (`0..255` and `0..65535`), so length fields
(`{LEN}`, `{STR}`) can use their full range.

**Byte Order:**

| Order                         | Description                                         |
|-------------------------------|-----------------------------------------------------|
| `BE` / `BIG_ENDIAN` (default) | Most significant byte first (network byte order)    |
| `LE` / `LITTLE_ENDIAN`        | Least significant byte first (standard for x86/x64) |

Built-in Tag Reference
----------------------

### Payload: `{CONTENT}`

Marks the location of the main value.

* Parameters: None.
* When a corrupted or truncated frame is read (a negative length or a length exceeding the
  remaining bytes), a `RuntimeException` is thrown with the message
  `Corrupted frame: invalid payload length <length>`.

### Length Prefix: `{LEN}`

Writes the payload length in bytes. During reading, tells the `{CONTENT}` segment how many
bytes to read.

* Parameters:
    * `type` (optional, default `INT`): Data type for the length field.
    * `order` (optional, default `BE`): Byte order.
* Examples:
    * `{LEN}` — 4 bytes, Big-Endian.
    * `{LEN(SHORT, LE)}` — 2 bytes, Little-Endian.

### Protocol Version: `{VER}`

Writes a fixed version byte and validates it during reading.

* Parameters:
    * `version` (required): Version byte in decimal or hexadecimal format.
* Examples:
    * `{VER(1)}` — expects and writes byte `0x01`.
    * `{VER(0x05)}` — expects and writes byte `0x05`. Throws an error on mismatch during
      reading.

### Timestamp: `{TS}`

Writes the current test execution time in milliseconds (Unix Timestamp). During reading,
these bytes are simply skipped.

* Parameters:
    * `type` (required): Data type for the timestamp (usually `LONG`).
    * `order` (required): Byte order.
* Example: `{TS(LONG, BE)}` — 8 bytes of timestamp.

### Checksum: `{CRC32}`

Computes a CRC32 checksum of all packet bytes written before this segment. Occupies exactly
4 bytes. On read the checksum is recomputed over the bytes read so far and verified against the
stored value; a mismatch throws an error.

* Parameters:
    * `order` (optional, default `BE`): Byte order for writing the checksum.
* Examples:
    * `{CRC32}` — 4 bytes CRC32, Big-Endian.
    * `{CRC32(LE)}` — 4 bytes, Little-Endian.

### Filler Bytes: `{FIX}`

Writes a fixed number of filler bytes. During reading, simply skips that distance.

* Parameters:
    * `size` (required): Number of bytes.
    * `value` (optional, default `0`): Byte value in decimal or hexadecimal format.
* Examples:
    * `{FIX(8, 0)}` — 8 bytes with value `0x00`.
    * `{FIX(2, 0xFF)}` — 2 bytes with value `0xFF`.

### Text Strings: `{STR}`

Embeds text markers in UTF-8 encoding.

* **Option 1 (Fixed text):** `{STR(text)}` — writes the bytes of the given string. During
  reading, verifies that they match.
    * Example: `{STR(PING)}` writes 4 bytes: `PING` in ASCII.
* **Option 2 (Length-prefixed string):** `{STR(text, type, order)}` — writes the string
  length first, then the string itself. On read, verifies that the string read matches the
  configured one.
    * Example: `{STR("Hello", INT, BE)}` writes 4 bytes of length (5) then 5 bytes of text.
* **Quote escaping:** If the text contains commas, parentheses, or quotes, wrap it in single
  `'` or double `"` quotes. Inside double quotes, escape inner quotes with `\"`.
    * Examples: `{STR('Hello, World')}`, `{STR("He said \"Hi\"")}`.

Creating Custom Binary Tags
---------------------------

If your project requires specific algorithms (e.g., applying an XOR mask to bytes or custom
encryption), you can extend the parser with your own tags. Implement `BinarySegment` and
`BinarySegmentProvider` and register the provider as a `@Component`.

A detailed example of creating the `{XORMASK(mask)}` tag is described in
[Extensibility](Extensibility.md#custom-binary-envelope-tags-binarysegmentprovider-binarysegment).

Practical Configuration Examples
--------------------------------

### Example 1: Redisson Format (Redis)

By default, many Redisson codecs prepend 8 empty filler bytes and an 8-byte Little-Endian
length to the serialized value. Point `bean-ref` at a `RedissonClient` (or at a Redisson `Codec`
bean, or at a bean with a `getCodec()` method) and describe the envelope on the schema component:

```yaml
integration:
  testing:
    redis:
      connections:
        redissonConnectionFactory:
          schemas:
            "redisson-value:":
              value:
                bean-ref: "redissonClient"
                # Describe the Redisson binary format
                binary-envelope: "{FIX(8, 0)}{LEN(LONG, LE)}{CONTENT}"
              hash-key:
                type: "org.springframework.data.redis.serializer.StringRedisSerializer"
              hash-value:
                type: "org.springframework.data.redis.serializer.StringRedisSerializer"
```

### Example 2: Custom Network Packet with Checksum (Redis)

Suppose your application communicates with Redis using the following protocol:

* Signature (magic bytes) — `AA` (1 byte)
* Version byte — `05` (1 byte)
* Creation timestamp — `LONG` (8 bytes, Big-Endian)
* Payload length — `SHORT` (2 bytes, Big-Endian)
* Payload (`CONTENT`)
* CRC32 checksum — 4 bytes (Big-Endian) at the very end.

The YAML configuration would look like this:

```yaml
schemas:
  "secure-queue:":
    value:
      type: json
      target-class: com.example.Packet
      binary-envelope: "AA{VER(5)}{TS(LONG, BE)}{LEN(SHORT, BE)}{CONTENT}{CRC32}"
    hash-key:
      type: string
    hash-value:
      type: string
```

### Example 3: Envelope in Kafka

The envelope can be applied to a record part or to a whole-record config whose source resolves to a
core serializer:

```yaml
topics:
  "order-out":
    deserializer:
      value:
        type: json
        target-class: com.example.OrderEvent
        binary-envelope: "{VER(1)}{LEN(INT, BE)}{CONTENT}"
```

---
[← Back to Home](../README.md)
