Customization and Extension in Java
===================================

[Russian version](../../ru/kafka/Extensibility.md)

If the configuration capabilities are not enough, write your own class and register it as a Spring
bean. Below are the extension points that require code.

Custom record serializer
------------------------

A whole record is an ordinary core converter. Implement `TestSerdeConverter<KafkaRecord, ProducerRecord,
TestSerdeContext>`:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Getter
public class PlainTextRecordSerializer implements TestSerdeConverter<KafkaRecord, ProducerRecord, TestSerdeContext> {

    private final Class<KafkaRecord> inputClass = KafkaRecord.class;
    private final Class<ProducerRecord> outputClass = ProducerRecord.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public ProducerRecord convert(KafkaRecord record, TestSerdeContext context) {
        return new ProducerRecord<>(
                record.getTopic(),
                record.getPartition(),
                toBytes(record.getKey()),
                toBytes(record.getValue()));
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

The deserializer is the reverse converter `ConsumerRecord → KafkaRecord`:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Getter
public class PlainTextRecordDeserializer
        implements TestSerdeConverter<ConsumerRecord, KafkaRecord, TestSerdeContext> {

    private final Class<ConsumerRecord> inputClass = ConsumerRecord.class;
    private final Class<KafkaRecord> outputClass = KafkaRecord.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    @Override
    public KafkaRecord convert(ConsumerRecord record, TestSerdeContext context) {
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
`io.github.dimkich.integration.testing.serde.providers.JsonProviderFactory`,
`StringProviderFactory`.

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
