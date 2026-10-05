Кастомизация и расширение через Java
====================================

[English version](../../en/kafka/Extensibility.md)

Если возможностей конфигурации не хватает, напишите свой класс и зарегистрируйте его как Spring-бин.
Ниже — точки расширения, для которых нужен код.

Свой сериализатор записи
------------------------

Запись целиком — это обычный конвертер ядра. Реализуйте `TestSerdeConverter<KafkaRecord, ProducerRecord,
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

Подключение к топику — через `bean-ref`:

```yaml
"custom-in":
  serializer:
    bean-ref: plainTextRecordSerializer
```

Свой десериализатор записи
--------------------------

Десериализатор — обратный конвертер `ConsumerRecord → KafkaRecord`:

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

Свои заголовки
--------------

Заголовки — тоже конвертеры: сериализатор `TestSerdeConverter<MultiValueMap<String, Object>, Headers,
TestSerdeContext>`, десериализатор — обратный.

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

Свой формат (TestSerdeProviderFactory)
--------------------------------------

Провайдер формата — общая точка расширения ядра serde: фабрика, которая для запрошенной пары
типов создаёт конвертер `input -> output`. Реализуйте `TestSerdeProviderFactory<I, O, C, R, P>`
(имя формата — `getName()`, класс конфигурации — `P`) и зарегистрируйте бин — он станет
доступен под своим именем и в настройке целиком, и в частях. Примеры:
`io.github.dimkich.integration.testing.serde.providers.JsonProviderFactory`,
`StringProviderFactory`.

В Kafka провайдер можно указывать и в настройке целиком, и в частях. Провайдеры, созданные для
заголовков, не должны зависеть от контекста сообщения — иначе при старте будет ошибка с подсказкой.

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
