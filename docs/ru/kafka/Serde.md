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

**База записи.** Поля `type`, `bean-ref`, `target-class`, `object-mapper-ref` и настройки
`spring-json`/`spring-xml` на уровне записи — это база для всех частей: при загрузке
конфигурации они вливаются в компоненты `value`, `key` и `headers`, а заданное в компоненте
значение перекрывает базу. Если база не задаёт формат, части используют значения по умолчанию:
`key` и `value` — строки, `headers` — plain.

```yaml
"order-out":
  deserializer:
    type: json
    target-class: com.example.kafka.OrderEvent
    key:
      type: string
```

**По частям.** Ключ, значение и заголовки настраиваются отдельно; части перекрывают базу по полям:

```yaml
"order-out":
  deserializer:
    type: json
    target-class: com.example.kafka.OrderEvent
    key:
      type: string
    headers:
      type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
```

Компоненты можно задавать и явно — база дополнит их незаданные поля: например,
`value: {binary-envelope: ...}` работает вместе с `type: json` на уровне записи.

Справочник провайдеров
----------------------

Общие для всех платформ провайдеры (`string`, `json`, `xml`, `yaml`, `bytes`), поля
`target-class`/`object-mapper-ref` и правила выбора источника описаны в
[обзоре Serde](../serde/README.md). В Kafka ядровые форматы доступны и целиком, и в частях;
платформенные — только целиком:

| `type`                                   | Где доступен   | Что делает                                                                                                                 |
|------------------------------------------|----------------|----------------------------------------------------------------------------------------------------------------------------|
| `string`, `json`, `xml`, `yaml`, `bytes` | запись и части | ядровые форматы — см. [обзор Serde](../serde/README.md)                                                                    |
| `spring-json`                            | запись и части | `JsonSerializer`/`JsonDeserializer` Spring Kafka с информацией о типе в заголовках; в `headers` — Spring-маппер заголовков |
| `spring-xml`                             | запись и части | то же самое для XML                                                                                                        |
| полное имя класса                        | запись и части | указанный класс создаётся и используется как есть                                                                          |
| `bean-ref`                               | запись и части | готовый Spring-бин используется как есть                                                                                   |

На уровне записи в `type`/`bean-ref` можно указать и нативный Kafka `Serializer`/`Deserializer`:
после мержа он станет serde всех частей, если часть не перекрыта своей конфигурацией.

`spring-json`/`spring-xml` можно применять и к отдельным частям: в `key`/`value` он работает как
`JsonSerializer`/`JsonDeserializer` (в `key` пишется `__KeyTypeId__`, в `value` — `__TypeId__`),
в `headers` — как Spring-маппер заголовков.

Если база не задана и настроен только `value`, то `key` остаётся строкой, а
`headers` — plain-заголовками.

`binary-envelope` — шаблон бинарного конверта (см. [Бинарные упаковки](../serde/Binary-Envelopes.md)) —
можно добавить только к частям `key`/`value`: core-провайдеру (`json`/`xml`/…) или нативному Kafka
`Serializer`/`Deserializer` (`type` с FQCN или `bean-ref`). На уровне записи целиком `binary-envelope`
не поддерживается: чтобы обернуть значение, задайте конверт в `value` (например,
`value: {binary-envelope: ...}` рядом с `type: json` на уровне записи). В `headers` конверт
не поддерживается: заголовки сериализуются по значению.

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

* **plain-формат (по умолчанию, когда база не задаёт формат заголовков)** — каждое значение
  записывается как строка; повторяющиеся значения сохраняются в порядке появления. Подходит для
  простых текстовых заголовков.
* **`DefaultKafkaHeaderMapper`** — Spring-маппер, который умеет работать с типами и корректно
  обрабатывает служебные заголовки `__TypeId__`, `spring_json_header_types`. Указывается явно:

```yaml
headers:
  type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
```

В заголовках допустимы только сериализаторы без контекста (строки, JSON, XML, bytes, native-классы).
`spring-json`/`spring-xml` в заголовках дают Spring-маппер; `binary-envelope` в заголовках
не поддерживается.

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
