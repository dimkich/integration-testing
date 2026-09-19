Записи Kafka: отправка и проверка
=================================

[English version](../../en/kafka/Records-and-Headers.md)

Модель KafkaRecord
------------------

Одно сообщение Kafka в тестах описывается типом `KafkaRecord`:

| Поле         | XML     | Описание                                                      |
|--------------|---------|---------------------------------------------------------------|
| `connection` | атрибут | имя подключения из конфигурации                               |
| `topic`      | атрибут | имя топика                                                    |
| `key`        | элемент | ключ сообщения (может отсутствовать)                          |
| `value`      | элемент | значение сообщения (может отсутствовать — это tombstone)      |
| `headers`    | элемент | заголовки; повторяющиеся теги накапливаются в список значений |
| `partition`  | элемент | номер партиции                                                |
| `offset`     | элемент | смещение в партиции                                           |
| `timestamp`  | элемент | время сообщения                                               |

Поля `partition`, `offset` и `timestamp` заполняются при чтении сообщений сниффером и обычно
исключаются из сравнения через `excluded-fields` (см. [Конфигурацию](Configuration.md)).

Отправка сообщений из теста (`<inboundMessage>`)
------------------------------------------------

Блок `<inboundMessage>` публикует сообщение в топик и тем самым запускает кейс. Это один из двух
взаимоисключающих способов запуска: если в тесте указаны и `<inboundMessage>`, и вызов метода
(`<bean>`/`<method>`), фреймворк выполнит только отправку сообщения, а метод не вызовет. Нужны оба
действия — разнесите их по разным кейсам или `Part`'ам.

```xml

<inboundMessage type="KafkaRecord" connection="kafka1" topic="order-in">
    <key>order-1</key>
    <value>{"orderId": 1, "item": "Book"}</value>
    <partition>0</partition>
    <headers>
        <source>test</source>
        <tag>first</tag>
        <tag>second</tag>
    </headers>
</inboundMessage>
```

Как это работает:

* сообщение сериализуется сериализатором, настроенным для топика (по умолчанию — строки);
* если время сообщения не задано, подставляется «виртуальное» время из инициализации
  (`<init type="DateTimeInit" .../>`) — это удобно для детерминированных проверок;
* отправка выполняется синхронно: к моменту старта кейса сообщение уже в топике;
* сообщению запоминается offset, чтобы потом отличить его от сообщений приложения
  (см. `ignore-inbound` в [Конфигурации](Configuration.md)).

Отправлять сообщения можно в несколько топиков — просто укажите несколько `<inboundMessage>` подряд.

Tombstone (ключ без значения) описывается пустым элементом:

```xml

<inboundMessage type="KafkaRecord" connection="kafka1" topic="order-in">
    <key>order-1</key>
</inboundMessage>
```

Проверка сообщений приложения (`<outboundMessage>`)
---------------------------------------------------

Фоновый потребитель читает все пользовательские топики и забирает всё, что отправило приложение.
Ожидания описываются блоками `<outboundMessage>`:

```xml

<outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
    <key>order-1</key>
    <value>{"orderId": 1, "status": "PROCESSED"}</value>
</outboundMessage>
```

Правила:

* в конце кейса пойманные сообщения сверяются с ожидаемыми с учётом `excluded-fields`; при
  расхождении показывается визуальный Diff;
* для проверки ответов приложения используйте основное подключение — сообщения, отправленные самим
  тестом, из результатов исключаются (`ignore-inbound: true` по умолчанию);
* если нужно видеть весь поток целиком, заведите отдельное подключение-«монитор» с
  `ignoreInbound: false` (см. [Конфигурацию](Configuration.md));
* несколько `<outboundMessage>` проверяются независимо — их количество не обязано совпадать с
  количеством вызовов в кейсе.

Проверка состояния приложения (`<dataStorageDiff>`)
---------------------------------------------------

`<outboundMessage>` показывает, что приложение **отправило**. Если нужно проверить, что оно
**приняло** или как изменилось его состояние, используйте обычный `<dataStorageDiff>` — например, по
хранилищу, которое ведёт ваша тестовая обвязка:

```xml

<dataStorageDiff type="MapStringKeyObjectValue">
    <receivedMessages type="MapStringKeyObjectValue">
        <msg-0 type="KafkaRecord" topic="order-in">
            <key>order-1</key>
            <value>{"orderId": 1, "item": "Book"}</value>
        </msg-0>
    </receivedMessages>
</dataStorageDiff>
```

Здесь `receivedMessages` — пример хранилища: в тестах модуля так называется бин, куда слушатель
складывает принятые сообщения. В вашем проекте это может быть любое хранилище (БД, кэш, коллекция
бина). Подробнее о диффах — в разделе [Инициализация тестов](../Initialization.md).

Заголовки
---------

Заголовки — это мультикарта: один и тот же заголовок может встречаться несколько раз.

```xml

<headers>
    <tag>first</tag>
    <tag>second</tag>
    <source>test</source>
</headers>
```

В этом примере заголовок `tag` получит список из двух значений в порядке появления, а `source` — список
из одного значения.

Особенности:

* в XML повторяющиеся теги с одним именем накапливаются в список (в JSON используйте массив);
* при сериализации заголовки записываются «как есть» (plain-формат) либо через маппер Spring — это
  зависит от настройки `headers` (см. [Serde](Serde.md));
* Spring-форматы могут добавлять служебные заголовки (`__TypeId__`, `spring_json_header_types`) и
  технические заголовки Kafka (`kafka_offset`, `kafka_receivedTimestamp` и др.) — их обычно
  исключают через `excluded-fields`.

Tombstone, партиции и время
---------------------------

* **Tombstone** — пустой `<value>` или пустой элемент целиком: проверяйте так же, как обычное
  сообщение, оставив `<value>` незаполненным.
* **Партиция** — указывайте `<partition>` в `<inboundMessage>`, если приложению важен конкретный
  раздел. При чтении поле заполняется автоматически.
* **Время** — при отправке подставляется виртуальное время теста; при чтении — реальное время
  брокера. Чтобы сравнение не падало, `timestamp` обычно добавляют в `excluded-fields`.

Ошибки десериализации
---------------------

Если сниффер не может разобрать сообщение приложения, кейс не падает сразу: запись сохраняется как
«сырая» (UTF-8) вместе с описанием ошибки, а исключение пробрасывается позже, при проверке. В
ожидании такую запись описывают блоком `<exception>`:

```xml

<outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
    <exception>
        <exceptionType>MismatchedInputException</exceptionType>
        <message>Cannot construct instance of `com.example.OrderEvent` ...</message>
    </exception>
    <key>error-invalid-json</key>
    <value>"{invalid json}"</value>
</outboundMessage>
```

Такой тест полезен для проверки поведения приложения при «плохих» данных. Если это не ожидаемая
ситуация — исправьте `deserializer` для топика (см. [Serde](Serde.md)) или проверьте формат данных.

Полный пример
-------------

```xml

<test type="Container">
    <test type="Case" name="Заказ обработан">
        <init type="DateTimeInit" dateTime="2026-05-31T11:30:00Z"/>

        <!-- Фреймворк отправляет заказ -->
        <inboundMessage type="KafkaRecord" connection="kafka1" topic="order-in">
            <key>order-1</key>
            <value>{"orderId": 1, "item": "Book"}</value>
            <headers>
                <source>test</source>
            </headers>
        </inboundMessage>

        <!-- Приложение обрабатывает заказ и отправляет событие -->
        <outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
            <key>order-1</key>
            <value>{"orderId": 1, "status": "PROCESSED"}</value>
        </outboundMessage>
    </test>

    <test type="Case" name="Заказ отклонён">
        <bean>orderService</bean>
        <method>reject</method>
        <request>order-2</request>

        <outboundMessage type="KafkaRecord" connection="kafka1" topic="order-rejected">
            <key>order-2</key>
            <value>{"orderId": 2, "reason": "EMPTY_ITEM"}</value>
        </outboundMessage>
    </test>
</test>
```

---
[← На главную](../README.md)
