Интеграционное тестирование Kafka
=================================

[English version](../../en/kafka/README.md)

Этот модуль позволяет тестировать работу приложения с Kafka. Сценарии описываются в XML или JSON:
тест сам публикует сообщения в топики, запускает логику приложения и проверяет, что приложение
отправило в ответ.

Как это работает?
-----------------

Работа с брокером полностью на стороне фреймворка:

1. **Отправка сообщений:** перед кейсом фреймворк публикует в топики сообщения из блоков
   `<inboundMessage>`.
2. **Выполнение теста:** запускается ваш сценарий — вызов метода бина или HTTP-запрос. Если кейс
   запускается сообщением из `<inboundMessage>`, шаг сводится к ожиданию обработки.
3. **Слежение за топиками:** фоновый потребитель (sniffer) читает все пользовательские топики и
   забирает всё, что отправило приложение.
4. **Проверка результатов:** в конце кейса фреймворк сравнивает пойманные сообщения с блоками
   `<outboundMessage>` и показывает разницу в виде удобного визуального сравнения (Diff).

Фреймворк также дожидается, что приложение действительно обработало сообщения, прежде чем завершить
кейс — подробнее в разделе [In-Flight трекинг и контроль лага](In-Flight-and-Lag.md).

Подключение к проекту
---------------------

Kafka поднимается автоматически через Testcontainers, поэтому для запуска тестов нужен Docker. Всё,
что требуется от тестового класса:

```java
package com.example.kafka;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.kafka.EnableTestKafka;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

@EnableTestKafka // <-- Эта аннотация включает поддержку тестов Kafka
@SpringBootTest
public class OrderKafkaIntegrationTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> kafkaTests() throws Exception {
        // Указываем путь к нашему XML-файлу с тестами
        return dynamicTestBuilder.build("tests/kafka-order.xml");
    }
}
```

`@EnableTestKafka` сам активирует подсистемы сообщений и ожиданий: свойства
`integration.testing.message.enabled` и `integration.testing.wait.completion.enabled` уже заданы в
`kafka.properties`, поэтому указывать их в `@SpringBootTest` не нужно.

Остаётся указать адрес брокера в `application-test.yml`. Embedded Kafka публикует его в свойстве
`embedded.kafka.brokerList`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${embedded.kafka.brokerList}   # клиенты приложения

integration:
  testing:
    kafka:
      connections:
        kafka1:
          bootstrapServers: ${embedded.kafka.brokerList}   # клиенты фреймворка
```

> **Совет:** основной и самый быстрый режим — `inflight = true` (по умолчанию): лаг считается по
> данным клиентов приложения, без обращений к брокеру. Режим
> `@EnableTestKafka(inflight = false)` (Admin API) используйте только тогда, когда клиенты
> приложения недоступны (например, SUT в другом процессе): он опрашивает брокер на каждой проверке
> лага и поэтому медленнее. Подробнее — [In-Flight трекинг и контроль лага](In-Flight-and-Lag.md).

Быстрый старт: Пишем первый тест
--------------------------------

Создайте файл `src/test/resources/tests/kafka-order.xml`. В примере используются настройки serde по
умолчанию (строковые key/value), поэтому дополнительная конфигурация топиков не нужна:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- @formatter:off -->
<test type="Container">
    <!-- Кейс 1: приложение запускается сообщением из Kafka -->
    <test type="Case" name="Заказ обработан">
        <inboundMessage type="KafkaRecord" connection="kafka1" topic="order-in">
            <key>order-1</key>
            <value>{"orderId": 1}</value>
        </inboundMessage>

        <!-- Проверяем, что приложение отправило результат -->
        <outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
            <key>order-1</key>
            <value>{"orderId": 1, "status": "PROCESSED"}</value>
        </outboundMessage>
    </test>

    <!-- Кейс 2: приложение запускается вызовом метода -->
    <test type="Case" name="Заказ обработан по вызову сервиса">
        <bean>orderService</bean>
        <method>processOrder</method>
        <request>order-2</request>

        <outboundMessage type="KafkaRecord" connection="kafka1" topic="order-out">
            <key>order-2</key>
            <value>{"orderId": 2, "status": "PROCESSED"}</value>
        </outboundMessage>
    </test>
</test>
```

Как читать этот тест:

* кейс запускается **либо** сообщением, **либо** вызовом метода — это два взаимоисключающих способа:
  * `<inboundMessage>` — сообщение, которое фреймворк публикует в Kafka, а обрабатывает слушатель
    приложения;
  * `<bean>`/`<method>`/`<request>` — обычный вызов метода бина приложения;
* если указать в одном кейсе и `<inboundMessage>`, и `<bean>`/`<method>`, вызов метода будет
  проигнорирован — фреймворк выполнит только отправку сообщения;
* `<outboundMessage>` — сообщение, которое приложение должно отправить в топик `order-out`.

Сверка `<outboundMessage>` выполняется после того, как фреймворк дождётся завершения обработки, поэтому
дополнительные `Thread.sleep` в тестах не нужны.

Что умеет модуль
----------------

| Возможность                                                       | Где почитать                                                           |
|-------------------------------------------------------------------|------------------------------------------------------------------------|
| Отправка сообщений из теста и проверка ответов приложения         | [Записи Kafka](Records-and-Headers.md)                                 |
| Заголовки, включая повторяющиеся, партиции и время                | [Записи Kafka](Records-and-Headers.md)                                 |
| Tombstone-записи (ключ без значения)                              | [Записи Kafka](Records-and-Headers.md)                                 |
| Настройка сериализации: String, JSON, XML, bytes, Spring JSON/XML | [Serde](Serde.md)                                                      |
| Несколько подключений к брокеру, regex-маски топиков              | [Конфигурация](Configuration.md)                                       |
| Ожидание, пока приложение обработает все сообщения                | [In-Flight трекинг и контроль лага](In-Flight-and-Lag.md)              |
| Топики, созданные во время теста, чтение по маске, ручной assign  | [In-Flight трекинг и контроль лага](In-Flight-and-Lag.md)              |
| Транзакции: commit, abort, отправка offset'ов                     | [In-Flight трекинг и контроль лага](In-Flight-and-Lag.md)              |
| Упавшие сообщения и незакоммиченные offset'ы                      | [Сценарии с ошибками и незакоммиченными offset'ами](Error-Handling.md) |
| Свои форматы и сериализаторы                                      | [Расширяемость](Extensibility.md)                                      |
| Диагностика типовых ошибок                                        | [Устранение неполадок](Troubleshooting.md)                             |

---
[← На главную](../README.md)
