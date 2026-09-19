Сериализация и десериализация (Serde)
=====================================

[English version](../../en/kafka/Serde.md)

Зачем это нужно
---------------

Фреймворк обменивается с брокером сырыми байтами: перед отправкой `<inboundMessage>` сообщение нужно
сериализовать, а пойманные сообщения приложения — десериализовать, чтобы показать их в Diff. Тем,
каким способом это делается, управляет настройка `serializer`/`deserializer` для топика. Клиенты
самого приложения при этом не подменяются — приложение продолжает работать со своим сериализатором.

Настройка по умолчанию
----------------------

Если для топика ничего не настроено, используются строки:

* `key` — строка (UTF-8);
* `value` — строка (UTF-8);
* `headers` — простые заголовки «как есть» (plain-формат).

Этого достаточно для быстрого старта и текстовых сообщений.

Две схемы настройки
-------------------

**Запись целиком.** Настраивается один раз для всего сообщения:

```yaml
"order-out":
  deserializer:
    type: json
    target-class: com.example.kafka.OrderEvent
```

**По частям.** Ключ, значение и заголовки настраиваются отдельно:

```yaml
"order-out":
  deserializer:
    key:
      type: string
    value:
      type: json
      target-class: com.example.kafka.OrderEvent
    headers:
      type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
```

Комбинировать схемы на одном уровне нельзя: `bean-ref` не сочетается с частями, `type` не сочетается с
`value` и т.д. Полная таблица допустимых комбинаций — в [Конфигурации](Configuration.md).

Справочник провайдеров
----------------------

| `type`            | Где доступен          | Что делает                                                                         |
|-------------------|-----------------------|------------------------------------------------------------------------------------|
| `string`          | запись и части        | строки UTF-8                                                                       |
| `json`            | запись и части        | Jackson JSON, поддерживает `target-class` и `object-mapper-ref`                    |
| `xml`             | запись и части        | Jackson XML, поддерживает `target-class` и `object-mapper-ref`                     |
| `yaml`            | запись и части        | YAML                                                                               |
| `bytes`           | запись и части        | сырые байты                                                                        |
| `spring-json`     | только запись целиком | `JsonSerializer`/`JsonDeserializer` Spring Kafka с информацией о типе в заголовках |
| `spring-xml`      | только запись целиком | то же самое для XML                                                                |
| полное имя класса | запись и части        | указанный класс создаётся и используется как есть                                  |
| `bean-ref`        | запись и части        | готовый Spring-бин используется как есть                                           |

Провайдеры `spring-json`/`spring-xml` не работают внутри `key`/`value`/`headers`: для частей
используйте `json`/`xml` — формат тот же, но без информации о типе.

Если для части задан только `value`, а `key`/`headers` не заданы, то `key` остаётся строкой, а
`headers` — plain-заголовками.

Настройки `spring-json` и `spring-xml`
--------------------------------------

Эти провайдеры пишут имя класса в заголовки Kafka, что позволяет десериализовать полиморфные данные:

| Поле                    | Где применяется | Описание                                                                         |
|-------------------------|-----------------|----------------------------------------------------------------------------------|
| `add-type-info-headers` | сериализация    | добавлять имя класса в заголовки (`true`/`false`); Spring по умолчанию добавляет |
| `use-type-info-headers` | десериализация  | читать имя класса из заголовков                                                  |
| `trusted-packages`      | десериализация  | список доверенных пакетов для разбора типа; `*` — разрешить все                  |
| `target-class`          | десериализация  | класс по умолчанию, если в заголовках типа нет                                   |

Пример полиморфной пары:

```yaml
"order-events":
  serializer:
    type: spring-json
    add-type-info-headers: true
  deserializer:
    type: spring-json
    use-type-info-headers: true
    trusted-packages: "*"
```

При записи сообщения в XML можно сразу указать тип значения, и тогда фреймворк сериализует его как
объект:

```xml

<outboundMessage type="KafkaRecord" connection="kafka1" topic="order-events">
    <key>order-1</key>
    <value type="OrderEventDto">
        <orderId>order-1</orderId>
        <status>PROCESSED</status>
    </value>
    <headers>
        <__TypeId__>com.example.kafka.OrderEventDto</__TypeId__>
    </headers>
</outboundMessage>
```

Сериализация заголовков
-----------------------

Заголовки можно описывать двумя способами:

* **plain-формат (по умолчанию)** — каждое значение записывается как строка; повторяющиеся значения
  сохраняются в порядке появления. Подходит для простых текстовых заголовков.
* **`DefaultKafkaHeaderMapper`** — Spring-маппер, который умеет работать с типами и корректно
  обрабатывает служебные заголовки `__TypeId__`, `spring_json_header_types`. Указывается явно:

```yaml
headers:
  type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
```

В заголовках допустимы только сериализаторы без контекста (строки, JSON, XML, bytes, native-классы).
Попытка использовать там провайдер записи целиком приведёт к понятной ошибке при старте.

Примеры конфигураций
--------------------

```yaml
topics:
  # Строки по умолчанию — настройка не нужна
  "text-in": { }

  # Целиком через JSON
  "json-in":
    serializer:
      type: json
  "json-out":
    deserializer:
      type: json
      target-class: com.example.kafka.JsonDto

  # По частям: строка в ключе, JSON в значении, Spring-маппер в заголовках
  "json-parts-in":
    serializer:
      key:
        type: string
      value:
        type: json
      headers:
        type: org.springframework.kafka.support.DefaultKafkaHeaderMapper

  # Байты
  "byte-in":
    serializer:
      type: bytes

  # Нативные классы Kafka
  "text-native-in":
    serializer:
      key:
        type: org.apache.kafka.common.serialization.StringSerializer
      value:
        type: org.apache.kafka.common.serialization.StringSerializer

  # Готовый бин сериализатора
  "custom-in":
    serializer:
      bean-ref: myCustomRecordSerializer
```

Если нужен свой формат
----------------------

Если ни один провайдер не подходит, можно написать собственные сериализатор, десериализатор или
провайдер формата — см. [Расширяемость](Extensibility.md).

---
[← На главную](../README.md)
