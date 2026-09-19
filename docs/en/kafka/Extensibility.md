Customization and Extension in Java
===================================

[Russian version](../../ru/kafka/Extensibility.md)

If the configuration capabilities are not enough, write your own class and register it as a Spring
bean. Below are the extension points that require code.

Custom record serializer
------------------------

Implement `KafkaRecordSerializer` — it turns a test message into bytes for the broker:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaRecordSerializer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PlainTextRecordSerializer implements KafkaRecordSerializer {

    @Override
    public ProducerRecord<byte[], byte[]> serialize(KafkaRecord message) {
        return new ProducerRecord<>(
                message.getTopic(),
                message.getPartition(),
                toBytes(message.getKey()),
                toBytes(message.getValue()));
    }

    private byte[] toBytes(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }
}
```

Attach it to a topic via `bean-ref`:

```yaml
"custom-in":
  serializer:
    bean-ref: plainTextRecordSerializer
```

Custom record deserializer
--------------------------

Implement `KafkaRecordDeserializer` — it turns a raw record captured by the sniffer into a
`KafkaRecord`:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaRecordDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PlainTextRecordDeserializer implements KafkaRecordDeserializer {

    @Override
    public KafkaRecord deserialize(ConsumerRecord<byte[], byte[]> record) {
        KafkaRecord message = new KafkaRecord();
        message.setTopic(record.topic());
        message.setKey(toString(record.key()));
        message.setValue(toString(record.value()));
        message.setPartition(record.partition());
        message.setOffset(record.offset());
        message.setTimestamp(record.timestamp());
        return message;
    }

    private String toString(byte[] data) {
        if (data == null) {
            return null;
        }
        return new String(data, StandardCharsets.UTF_8);
    }
}
```

```yaml
"custom-out":
  deserializer:
    bean-ref: plainTextRecordDeserializer
```

Custom headers
--------------

Header serialization is handled by `KafkaHeaderSerializer` and `KafkaHeaderDeserializer`:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaHeaderSerializer;
import org.apache.kafka.common.header.Headers;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

@Component
public class UpperCaseHeaderSerializer implements KafkaHeaderSerializer {

    @Override
    public void serialize(MultiValueMap<String, Object> source, Headers target) {
        source.forEach((name, values) -> values.forEach(value -> target.add(
                name,
                String.valueOf(value).toUpperCase().getBytes(StandardCharsets.UTF_8))));
    }
}
```

```yaml
headers:
  bean-ref: upperCaseHeaderSerializer
```

A custom format (TestSerdeProvider)
-----------------------------------

A format provider creates a "serializer + deserializer" pair and is registered under its own name. The
format can then be used in `type`:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class Base64SerdeProvider implements TestSerdeProvider<SerdeProperties> {

    @Override
    public String getName() {
        return "base64";
    }

    @Override
    public Object createSerializer(SerdeProperties config) {
        return new Base64Serializer();
    }

    @Override
    public Object createDeserializer(SerdeProperties config) {
        return new Base64Deserializer();
    }

    private static class Base64Serializer implements TestSerdeSerializer<Object, SerdeContext> {

        @Override
        public Class<SerdeContext> getContextClass() {
            return SerdeContext.class;
        }

        @Override
        public byte[] serialize(Object data, SerdeContext context) {
            if (data == null) {
                return null;
            }
            return Base64.getEncoder().encode(String.valueOf(data).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static class Base64Deserializer implements TestSerdeDeserializer<Object, SerdeContext> {

        @Override
        public Class<SerdeContext> getContextClass() {
            return SerdeContext.class;
        }

        @Override
        public Object deserialize(byte[] data, SerdeContext context) {
            if (data == null) {
                return null;
            }
            return new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
        }
    }
}
```

```yaml
"order-out":
  deserializer:
    value:
      type: base64
```

The provider must be a Spring bean: if your test package is not component-scanned, declare it via
`@Bean` in the test configuration. Providers created for headers must not depend on message context —
otherwise startup fails with a hint.

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
