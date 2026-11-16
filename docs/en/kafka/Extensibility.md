Customization and Extension in Java
===================================

[Russian version](../../ru/kafka/Extensibility.md)

If the configuration capabilities are not enough, write your own class and register it as a Spring
bean. Below are the extension points that require code.

Custom part converters
----------------------

There is no whole-record converter: a record is always assembled from the three components —
`key`, `value` and `headers` (`KafkaRecordSerdeFactory`). Register a converter for a component:
for value and key use `TestSerdeConverter<Object, ByteBuffer, KafkaSerdeContext>`, and for headers
see “Custom headers”.

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
@Getter
public class PlainTextValueSerializer
        implements TestSerdeConverter<Object, ByteBuffer, KafkaSerdeContext> {

    private final Class<Object> inputClass = Object.class;
    private final Class<ByteBuffer> outputClass = ByteBuffer.class;
    private final Class<KafkaSerdeContext> contextClass = KafkaSerdeContext.class;

    @Override
    public ByteBuffer convert(Object value, KafkaSerdeContext context) {
        if (value == null) {
            return null;
        }
        return ByteBuffer.wrap(value.toString().getBytes(StandardCharsets.UTF_8));
    }
}
```

Attach it to the value component:

```yaml
"custom-in":
  serializer:
    value:
      bean-ref: plainTextValueSerializer
```

The reverse converter for reading:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
@Getter
public class PlainTextValueDeserializer
        implements TestSerdeConverter<ByteBuffer, Object, KafkaSerdeContext> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<KafkaSerdeContext> contextClass = KafkaSerdeContext.class;

    @Override
    public Object convert(ByteBuffer data, KafkaSerdeContext context) {
        if (data == null) {
            return null;
        }
        return StandardCharsets.UTF_8.decode(data.duplicate()).toString();
    }
}
```

```yaml
"custom-out":
  deserializer:
    value:
      bean-ref: plainTextValueDeserializer
```

Use `key:` instead of `value:` for a key converter and `headers:` for headers.

Custom headers
--------------

Headers are converters too: the serializer is `TestSerdeConverter<MultiValueMap<String, Object>,
Headers, TestSerdeContext>`, the deserializer is the reverse direction.

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

@Component
@Getter
public class UpperCaseHeaderSerializer
        implements TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> {

    private final Class<MultiValueMap> inputClass = MultiValueMap.class;
    private final Class<Headers> outputClass = Headers.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    @SuppressWarnings("unchecked")
    public Headers convert(MultiValueMap map, TestSerdeContext context) {
        Headers headers = new RecordHeaders();
        ((MultiValueMap<String, Object>) map).forEach((name, values) -> values.forEach(value -> headers.add(
                name,
                String.valueOf(value).toUpperCase().getBytes(StandardCharsets.UTF_8))));
        return headers;
    }
}
```

```yaml
headers:
  bean-ref: upperCaseHeaderSerializer
```

A custom format (TestSerdeProviderFactory)
------------------------------------------

A format provider is a shared extension point of the serde core: a factory that creates an
`input -> output` converter for the requested type pair. Implement `TestSerdeProviderFactory<I, O, C, R, P>`
(the format name is `getName()`, the config class is `P`) and register the bean — it becomes
available under its name both in a whole-record config and in parts. Examples:
`io.github.dimkich.integration.testing.serde.providers.JsonSerializerFactory` /
`JsonDeserializerFactory`, `StringSerializerFactory` / `StringDeserializerFactory`.
The binary side of such a converter is `ByteBuffer`; at the Kafka boundary it becomes `byte[]`
inside the native record serializer.

In Kafka a provider can be used both in a whole-record config and in parts. Providers created for
headers must not depend on message context — otherwise startup fails with a hint.

Extra Kafka client properties
-----------------------------

Properties that are not covered by the tables are passed through `properties` as is:

```yaml
connections:
  secure:
    bootstrapServers: ${embedded.kafka.brokerList}
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      acks: all
```

Custom message subtypes
-----------------------

If you extend the message model with your own classes, register them through `TestSetupModule`, just
as the Kafka module itself does for `KafkaRecord`. See
[TestSetupModule Configuration](../TestSetupModule.md) for details.

---
[← Back to Home](../README.md)
