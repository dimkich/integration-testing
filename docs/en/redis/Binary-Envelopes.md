Working with Binary Envelopes
=============================

[Russian version](../../ru/redis/Binary-Envelopes.md)

In real projects, data in Redis is often not stored in plain form (e.g., JSON or a plain string).
Instead, it is wrapped in internal binary envelopes by client libraries or custom protocols.

Typical examples:

* Redisson automatically prepends a service header (codec marker, timestamps, or length) to
  your value.
* Custom protocols may require a signature (magic bytes), protocol version, payload length
  prefix at the start, and a checksum (CRC32) at the end.

The framework solves this declaratively using a template parser. You can describe the binary
envelope structure directly in `application-test.yml` using a simple text template.

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
* **Payload (`{CONTENT}`):** Your main serialized value (e.g., a DTO's JSON). Exactly one
  such segment must exist in the template.
* **Footer:** All segments declared after the `{CONTENT}` tag.

Template Syntax
---------------

A template is a string containing hexadecimal characters and control tags in curly braces `{}`.

### Plain Bytes (Static Hex Characters)

Any characters outside curly braces are treated as hexadecimal bytes. They write fixed markers.

* **Example:** `524453` writes 3 bytes: `0x52 0x44 0x53` (ASCII string "RDS"). During
  reading, these bytes are simply skipped.

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
4 bytes.

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
  reading, skips them.
    * Example: `{STR(PING)}` writes 4 bytes: `PING` in ASCII.
* **Option 2 (Length-prefixed string):** `{STR(text, type, order)}` — writes the string
  length first, then the string itself.
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
[Extensibility.md](Extensibility.md#custom-binary-format-tags-binarysegmentprovider-binarysegment).

Practical Configuration Examples
--------------------------------

### Example 1: Redisson Format

By default, many Redisson codecs prepend 8 empty filler bytes and an 8-byte Little-Endian
length to the serialized value:

```yaml
integration:
  testing:
    redis:
      connections:
        redissonConnectionFactory:
          defaultSchema:
            bean-ref: "redissonClient"
            # Describe the Redisson binary format
            valueBinaryFormat: "{FIX(8, 0)}{LEN(LONG, LE)}{CONTENT}"
```

### Example 2: Custom Network Packet with Checksum

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
    class-ref: io.github.dimkich.integration.testing.redis.schema.ComposedRedisDataSchema
    valueBinaryFormat: "AA{VER(5)}{TS(LONG, BE)}{LEN(SHORT, BE)}{CONTENT}{CRC32}"
```

---
[← Back to Home](../README.md)
