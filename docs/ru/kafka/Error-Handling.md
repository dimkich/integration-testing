Сценарии с ошибками и незакоммиченными offset'ами
=================================================

[English version](../../en/kafka/Error-Handling.md)

Почему тест падает на сообщении, которое приложение «пропустило»
----------------------------------------------------------------

Перед проверками фреймворк ждёт, пока все сообщения будут обработаны. В режиме `inflight = true`
сообщение считается обработанным, только если группа потребителей закоммитила offset дальше него:
пока `endOffset > committedOffset` хотя бы для одной подписанной партиции живой группы, лаг
ненулевой. Режим `inflight = false` задаёт тот же вопрос брокеру через Admin API.

Ожидание ограничено `lag-polling-timeout-ms` (по умолчанию 10000 мс; не путайте с
`startup-stabilization-timeout-seconds`, 30 секунд — этот таймаут ждёт готовности групп потребителей
перед каждым тестом). Когда время выходит, кейс падает:

```text
Kafka wait completion timeout. Unprocessed messages remaining on broker.
```

Если приложение обработало ошибку, но не коммитит offset пропущенного сообщения
(`AckMode.MANUAL` без `acknowledge()`, ручной коммит только при успехе, тест логики rollback и так
далее), лаг сам не исчезнет. Есть два пути.

Путь 1. Заставить приложение закоммитить пропущенное сообщение (рекомендуется)
-------------------------------------------------------------------------------

Приложение должно закоммитить offset записи, которую решило пропустить, — иначе потребитель
никогда не двинется дальше, а брокер будет показывать по ней лаг.

Spring Kafka, обычный слушатель:

```java

@Bean
DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
    DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template));
    handler.setCommitRecovered(true);
    return handler;
}
```

`commitRecovered = true` указывает Spring Kafka закоммитить offset восстановленной (например,
отправленной в DLQ) записи. Флаг учитывается при `AckMode.MANUAL_IMMEDIATE`; при других режимах
контейнер сам коммитит после обработки ошибки, если не выставлен `setAckAfterHandle(false)`.
Проверьте оба флага, если лаг не исчезает.

Spring Kafka, транзакционный (EOS) слушатель:

```java

@Bean
DefaultAfterRollbackProcessor<Object, Object> rollbackProcessor(KafkaTemplate<Object, Object> template) {
    return new DefaultAfterRollbackProcessor<>(
            new DeadLetterPublishingRecoverer(template),
            SeekUtils.DEFAULT_BACK_OFF,
            template,
            true);
}
```

`DefaultAfterRollbackProcessor` вызывается при откате транзакционного контейнера. С
`commitRecovered = true` он отправляет offset пропущенной записи через `sendOffsetsToTransaction`,
поэтому нужен транзакционный `KafkaOperations` — иначе флаг игнорируется. Для нетранзакционного
приложения используйте `DefaultErrorHandler` выше.

После этого фреймворк видит закоммиченный offset, и кейс завершается без задержек.

Путь 2. «Аварийный люк»: принудительный коммит из теста
-------------------------------------------------------

Иногда оставленный лаг — ожидаемое поведение:

* тест проверяет логику rollback или ручного коммита;
* приложение сознательно не подтверждает упавшую запись;
* конфигурацию приложения сейчас нельзя изменить.

> ⚠️ **Важно:** это Java-код, требующий доступа к классам Spring Kafka — привлекайте разработчика.
> Если конфигурацию приложения изменить всё-таки можно, используйте Путь 1.

Тогда перехватите завершение обработки ошибки Spring Kafka и закоммитьте offset из теста.
`handleRemaining` выполняется в потоке консьюмера, поэтому `commitSync` там безопасен.

### 1. Обработчик (Action)

`MethodAction` — обычный Java-класс с публичным конструктором без аргументов (не Spring-бин).

```java
package com.example.test;

import io.github.dimkich.integration.testing.execution.hook.MethodAction;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class KafkaForceCommitAction implements MethodAction {

    private static final Logger log = Logger.getLogger(KafkaForceCommitAction.class.getName());

    @Override
    @SuppressWarnings("unchecked")
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        // DefaultErrorHandler.handleRemaining(Exception, List<ConsumerRecord<?, ?>>, Consumer<?, ?>, MessageListenerContainer)
        List<ConsumerRecord<?, ?>> records = (List<ConsumerRecord<?, ?>>) args[1];
        Consumer<?, ?> consumer = (Consumer<?, ?>) args[2];
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        for (ConsumerRecord<?, ?> record : records) {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());
            offsets.merge(partition, new OffsetAndMetadata(record.offset() + 1),
                    (left, right) -> left.offset() >= right.offset() ? left : right);
        }
        try {
            consumer.commitSync(offsets);
        } catch (Exception e) {
            log.fine("Force commit failed: " + e);
        }
    }
}
```

Действие коммитит максимум `offset + 1` по каждой партиции среди всех записей, переданных
обработчику: так пропускается весь неудачный poll, и следующий тест не унаследует лаг. Коммита
только первой записи недостаточно: более позднее сообщение в той же партиции оставит лаг
ненулевым.

### 2. Аннотация

```java

@EnableTestKafka
@SpringBootTest
@OnMethodExit(
        pointcut = "t.inherits('org.springframework.kafka.listener.CommonErrorHandler') && m.name('handleRemaining')",
        action = KafkaForceCommitAction.class)
public abstract class BaseKafkaIntegrationTest {
    // ...
}
```

`@OnMethodExit` помечен `@Inherited`, поэтому его можно повесить на базовый тестовый класс.
Pointcut покрывает любой `CommonErrorHandler`, включая `DefaultErrorHandler`.

### Как это работает в рантайме

1. Тест отправляет сообщение, которое приложение не может обработать. Фреймворк видит лаг и ждёт.
2. Приложение исчерпывает ретраи и передаёт запись обработчику ошибок.
3. Действие коммитит offset пропущенной записи.
4. Фреймворк видит новый закоммиченный offset и продолжает кейс. Брокер остаётся чистым для
   следующих тестов.

### Ограничения

* Сообщение пропускается для группы — приложение его не обработает. Используйте «аварийный люк»,
  только когда сценарий этого и ожидает.
* Не применяйте его к транзакционным (EOS) слушателям: прямой коммит потребителя вместе с
  `sendOffsetsToTransaction` нарушает контракт транзакции. Исправляйте логику транзакции.
* Для batch-слушателей точка перехвата — `handleBatch`, а `args[1]` — это `ConsumerRecords<?, ?>`,
  а не список записей.
* Ошибки действия не ломают приложение: `MethodHookAdvice` логирует их на уровне `FINE`.
* Если хук не срабатывает, потому что класс обработчика ошибок был загружен раньше, добавьте
  `@RepeatInstrumentation({"org.springframework.kafka"})` — см.
  [Хуки на методы](../Method-Hooks.md#устранение-неполадок).

Рабочий пример
--------------

В тестах модуля есть готовый сценарий:

* `integration-testing-kafka/src/test/java/io/github/dimkich/integration/testing/kafka/uncommitted/KafkaForceCommitAction.java`
* `integration-testing-kafka/src/test/java/io/github/dimkich/integration/testing/kafka/uncommitted/UncommittedOffsetConfig.java`
* `integration-testing-kafka/src/test/java/io/github/dimkich/integration/testing/kafka/uncommitted/SutErrorHandlingExamples.java`
  — конфигурация из Пути 1 (проверяется компиляцией, в тестовый контекст не входит)
* `integration-testing-kafka/src/test/java/io/github/dimkich/integration/testing/kafka/KafkaInFlightTest.java`
  и `KafkaAdminTest.java` — здесь объявлен `@OnMethodExit`; оба гоняют `kafka.xml`
* `integration-testing-kafka/src/test/resources/kafka.xml` — кейсы «Poison message: offset is
  force-committed by the hook» и «Next case is not blocked by leftover lag»

См. также
----------

* [In-Flight трекинг и контроль лага](In-Flight-and-Lag.md) — как считается лаг и какие есть
  таймауты.
* [Хуки на методы](../Method-Hooks.md) — механизм `@OnMethodExit` / `MethodAction`.
* [Устранение неполадок](Troubleshooting.md) — другие причины timeout ожидания.

---
[← На главную](../README.md)
