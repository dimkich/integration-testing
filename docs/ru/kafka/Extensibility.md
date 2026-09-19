Кастомизация и расширение через Java
====================================

[English version](../../en/kafka/Extensibility.md)

Если возможностей конфигурации не хватает, напишите свой класс и зарегистрируйте его как Spring-бин.
Ниже — точки расширения, для которых нужен код.

Свой сериализатор записи
------------------------

Реализуйте `KafkaRecordSerializer` — он превращает сообщение теста в байты для брокера:

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

Подключение к топику — через `bean-ref`:

```yaml
"custom-in":
  serializer:
    bean-ref: plainTextRecordSerializer
```

Свой десериализатор записи
--------------------------

Реализуйте `KafkaRecordDeserializer` — он превращает сырую запись, пойманную сниффером, в
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

Свои заголовки
--------------

За сериализацию заголовков отвечают `KafkaHeaderSerializer` и `KafkaHeaderDeserializer`:

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

Свой формат (TestSerdeProvider)
-------------------------------

Провайдер формата создаёт пару «сериализатор + десериализатор» и регистрируется под собственным
именем. Такой формат затем можно указывать в `type`:

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

Провайдер должен быть Spring-бином: если ваш тестовый пакет не попадает в сканирование, объявите его
через `@Bean` в конфигурации теста. Провайдеры, созданные для заголовков, не должны зависеть от
контекста сообщения — иначе при старте будет ошибка с подсказкой.

Дополнительные свойства Kafka-клиента
-------------------------------------

Свойства, которых нет в таблицах, передаются как есть через `properties`:

```yaml
connections:
  secure:
    bootstrapServers: ${embedded.kafka.brokerList}
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      acks: all
```

Свои подтипы сообщений
----------------------

Если вы расширяете модель сообщений собственными классами, зарегистрируйте их через `TestSetupModule`,
как это делает сам модуль Kafka для `KafkaRecord`. Подробности —
[Конфигурация TestSetupModule](../TestSetupModule.md).

---
[← На главную](../README.md)
