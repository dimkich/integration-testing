Кастомизация и расширение через Java
====================================

[English version](../../en/kafka/Extensibility.md)

Если возможностей конфигурации не хватает, напишите свой класс и зарегистрируйте его как Spring-бин.
Ниже — точки расширения, для которых нужен код.

Свои конвертеры частей
----------------------

Отдельного конвертера записи целиком нет: запись всегда собирается из трёх компонентов —
`key`, `value` и `headers` (`KafkaRecordSerdeFactory`). Свой конвертер регистрируется для
компонента: для value и key — `TestSerdeConverter<Object, ByteBuffer, KafkaSerdeContext>`, для
headers — см. раздел «Свои заголовки».

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

Подключение к компоненту value:

```yaml
"custom-in":
  serializer:
    value:
      bean-ref: plainTextValueSerializer
```

Обратный конвертер для чтения:

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

Для ключа используйте `key:` вместо `value:`; для заголовков — `headers:`.

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
`io.github.dimkich.integration.testing.serde.providers.JsonSerializerFactory` /
`JsonDeserializerFactory`, `StringSerializerFactory` / `StringDeserializerFactory`.
Бинарная сторона такого конвертера — `ByteBuffer`; на границе с Kafka она превращается в
`byte[]` нативным сериализатором записи.

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
