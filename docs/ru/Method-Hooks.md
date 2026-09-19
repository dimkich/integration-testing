# Хуки на методы: `@OnMethodEnter` и `@OnMethodExit`

## Введение

Иногда тесту нужно не просто **дождаться** фоновой работы, а **увидеть или изменить** то, что происходит внутри
методов приложения: зафиксировать факт вызова, проверить аргументы, заметить исключение, замерить длительность или
подменить данные «на лету».

Система **Method Hooks** даёт для этого декларативный механизм. На тестовом классе объявляется аннотация с
pointcut-выражением, и фреймворк через ByteBuddy «врезает» в подходящие методы приложения невидимый адвайс (advice),
который перед входом в метод или при выходе из него вызывает ваш Java-код — **`MethodAction`**. Основной код приложения
при этом не меняется: ни тестовых ветвлений, ни публичных методов «только для тестов».

### Место среди других механизмов фреймворка

| Механизм                                                                          | Уровень                     | Когда работает                                      | Для чего                                           |
|-----------------------------------------------------------------------------------|-----------------------------|-----------------------------------------------------|----------------------------------------------------|
| `BeforeTest` / `AfterTest` / `TestConverter`                                      | Spring-бины теста           | вокруг выполнения теста (Container/Case/Part)       | setup/cleanup теста, нормализация ожидаемых данных |
| **`@OnMethodEnter` / `@OnMethodExit`**                                            | байт-код методов приложения | при каждом вызове подходящего метода                | наблюдение, проверки, замеры, эмуляция             |
| `@FutureLikeAwait`, `@MethodCountingAwait`, `@MethodPairAwait`, `@QueueLikeAwait` | байт-код + ожидание         | в фазе проверки, блокирует тест до завершения задач | синхронизация с асинхронностью                     |

Хуки **не ждут** завершения задач — они лишь выполняют действие в момент вызова. Если нужно дождаться результата,
используйте [систему Wait-Completion](wait-completion.md); хуки и стратегии ожидания спокойно сосуществуют на одном
тестовом классе, потому что это независимые `InstrumentationPlugin`.

> **См. также:** DSL `pointcut` и `when`, который используется хуками, — общий с системой Wait-Completion. Полные
> справочники функций `t`, `m`, `o`, `a` приведены ниже на этой странице.

---

## Быстрый старт

### 1. Тестируемый сервис

```java
package com.example.payment;

public class PaymentService {
    public Receipt pay(String accountId, BigDecimal amount) {
        // ... бизнес-логика ...
        return new Receipt(accountId, amount);
    }
}
```

### 2. Действие

`MethodAction` — обычный Java-класс с **публичным конструктором без аргументов**. Spring-бин из него не создаётся,
поэтому зависимости в него не инжектируются (см. раздел «MethodAction»).

```java
package com.example.test;

import io.github.dimkich.integration.testing.execution.hook.MethodAction;

import java.lang.reflect.Executable;
import java.util.Arrays;

public class RecordPaymentAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        HookLog.add("pay(" + Arrays.toString(args) + ")");
    }
}
```

### 3. Тестовый класс

```java
package com.example.test;

import io.github.dimkich.integration.testing.execution.hook.OnMethodEnter;
import io.github.dimkich.integration.testing.execution.hook.OnMethodExit;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@OnMethodEnter(
        pointcut = "t.name('com.example.payment.PaymentService') && m.name('pay')",
        action = RecordPaymentAction.class)
@OnMethodExit(
        pointcut = "t.name('com.example.payment.PaymentService') && m.name('pay')",
        action = RecordResultAction.class)
class PaymentServiceTest {
    // ... @IntegrationTesting / DynamicTestBuilder и сами сценарии ...
}
```

Аннотации хуков помечены мета-аннотацией `@IntegrationTesting`, поэтому на классе с `@SpringBootTest` этого уже
достаточно: JUnit-расширение установит агент до создания Spring-контекста, и методы `PaymentService` будут
проинструментированы.

---

## Справочник аннотаций

Обе аннотации находятся в пакете `io.github.dimkich.integration.testing.execution.hook` и имеют одинаковый набор
атрибутов.

| Атрибут    | Тип                             | Обязательный | По умолчанию | Описание                                                                                            |
|------------|---------------------------------|--------------|--------------|-----------------------------------------------------------------------------------------------------|
| `pointcut` | `String`                        | да           | —            | DSL-выражение: какие классы и методы инструментировать. Вычисляется один раз при загрузке класса    |
| `when`     | `String`                        | нет          | `"true"`     | DSL-выражение-фильтр, вычисляется **при каждом вызове** метода; при `false` действие не выполняется |
| `action`   | `Class<? extends MethodAction>` | да           | —            | Класс действия; создаётся один раз на аннотацию при настройке инструментации                        |

### `@OnMethodEnter`

Действие выполняется **в начале метода**, до его тела (для конструктора — до выполнения тела конструктора).
Инструментирует как обычные, так и статические методы и конструкторы.

### `@OnMethodExit`

Действие выполняется **при выходе из метода** — и при нормальном возврате, и при исключении. На конструкторы
**не ставится**: ByteBuddy не может вызвать адвайс при исключении из конструктора до завершения инициализации
объекта. Для конструкторов используйте `@OnMethodEnter`.

### Общие свойства

* **`@Repeatable`** — на одном тестовом классе можно объявить сколько угодно enter- и exit-хуков; каждый из них
  независим и имеет собственное действие и собственный `when`.
* **`@Inherited`** — аннотации видны и на наследниках тестового класса.
* **`@Target(TYPE)`**, `@Retention(RUNTIME)` — аннотации ставятся только на класс.
* **Мета-аннотация `@IntegrationTesting`** — объявление хука включает инфраструктуру тестирования; отдельная
  `@IntegrationTesting` не требуется (но и не мешает).

Порядок применения нескольких аннотаций — порядок объявления. Не полагайтесь на него для логически связанных хуков:
если важен строгий порядок, объедините логику в одно действие.

---

## `MethodAction`

```java

@FunctionalInterface
public interface MethodAction {
    void execute(Object target, Executable method, Object[] args,
                 Object returnValue, Throwable thrown) throws Exception;
}
```

### Доступность параметров

| Параметр      | Enter, обычный метод | Enter, static    | Enter, конструктор            | Exit, обычный метод                    | Exit, static          |
|---------------|----------------------|------------------|-------------------------------|----------------------------------------|-----------------------|
| `target`      | экземпляр            | `null`           | `null` (объект ещё не создан) | экземпляр                              | `null`                |
| `method`      | `Method`             | `Method`         | `Constructor`                 | `Method`                               | `Method`              |
| `args`        | аргументы вызова     | аргументы вызова | аргументы конструктора        | аргументы вызова                       | аргументы вызова      |
| `returnValue` | `null`               | `null`           | `null`                        | результат или `null` (void/исключение) | результат или `null`  |
| `thrown`      | `null`               | `null`           | `null`                        | исключение или `null`                  | исключение или `null` |

Дополнительно:

* `method` — это `java.lang.reflect.Executable`: либо `Method`, либо `Constructor`. Проверяйте через
  `method instanceof Constructor`, как в примерах ниже.
* `args` всегда не `null` (возможно пустой массив). **Изменения элементов массива видит вызываемый метод** — это
  позволяет подменять аргументы, но требует осторожности.
* `returnValue` в exit-хуке может быть `null` и при нормальном завершении `void`-метода; отличить исключение от
  нормального выхода можно по `thrown`.

### Важные свойства действий

* **Action — не Spring-бин.** Экземпляр создаётся один раз на аннотацию через конструктор без аргументов
  (`getDeclaredConstructor().newInstance()`). `@Autowired`/конструкторная инъекция работать не будут. Для доступа к
  данным используйте статические коллекторы, «держатель» `ApplicationContext` или собственные статические сервисы.
* **Один экземпляр на все потоки.** Действие вызывается в потоках приложения (SUT), а не в потоке теста. Если
  действие хранит состояние, обеспечьте потокобезопасность (`synchronized`, `ConcurrentHashMap`, `AtomicReference`,
  `ThreadLocal`).
* **Исключения из действия не ломают приложение.** Адвайс оборачивает вызов `action.execute(...)` в `try/catch` и
  пишет ошибку в лог `MethodHookAdvice` на уровне `FINE` (`java.util.logging`). Тест при этом **не упадёт** — если
  ошибка в действии должна валить тест, фиксируйте её самостоятельно (например, в статическом коллекторе и проверяйте
  его в сценарии).
* **Ошибка в `when` — исключение.** В отличие от действия, вычисление `when` находится вне `try/catch`: исключение
  (например, `ClassCastException` из-за `asInt()` на не-числе) **пробросится в вызываемый метод приложения**. Держите
  `when` простым и защищайте проверки через `o.isNull()`/`a.size()`.

---

## Синтаксис `pointcut` и `when`

Хуки используют общий DSL фреймворка: `pointcut` (переменные `t` и `m`) описывает, какие классы и методы
инструментировать, а `when` (переменные `o` и `a`) — при каких вызовах выполнять действие.

```text
t.name('com.example.payment.PaymentService') && m.name('pay')
t.inherits('com.example.worker.BaseWorker') && m.name('process')
t.name('com.example.Order') && m.isConstructor()
```

```text
o.isSameClass(com.example.CriticalWorker.class)
a.arg(0).asString().startsWith('test-')
!o.isNull() && o.call('isEnabled').asBoolean()
```

При работе с хуками важно помнить:

* `when` вычисляется при **каждом** вызове метода — держите его дешёвым;
* ошибка в `when` пробрасывается в вызываемый метод приложения;
* в enter-хуке конструктора и static-метода переменная `o` пуста (`o.isNull() == true`);
* если в `pointcut` нет условий на метод (`m.*`), инструментируются **все** методы класса (а для enter — ещё и
  конструкторы).

Полный синтаксис, все функции переменных `t`, `m`, `o`, `a` и рекомендации по производительности — на странице
[DSL выражений](Expression-DSL.md).

---

## Модель выполнения

### Как устанавливается инструментация

1. JUnit-расширение `JunitExtension` (подключается через `@IntegrationTesting`) запускается с наивысшим приоритетом и
   **до создания Spring-контекста** устанавливает Java-агент.
2. `MethodHookPlugin` (реализация SPI `InstrumentationPlugin`, находится через `ServiceLoader`) проверяет, есть ли на
   тестовом классе хуки. Если есть — для каждой аннотации он компилирует `pointcut`, создаёт `PointcutSettings`
   (предикат `when` + экземпляр `action`) и добавляет ByteBuddy-адвайс в общий агент.
3. Классы, загружаемые **после** этого, инструментируются «на лету». Классы, которые уже были загружены до установки
   агента, повторно не трансформируются — для них применяется `@RepeatInstrumentation` (см. «Устранение неполадок»).

### Жизненный цикл вызова

```text
                      Вызов метода приложения
                                │
                                ▼
                 ┌── @OnMethodEnter адвайс ──┐
                 │   when == true ?          │
                 │   action.execute(         │
                 │      target, method, args,│
                 │      null, null)          │
                 └────────────┬──────────────┘
                              ▼
                       Тело метода
                       │        │
              нормальный│        │исключение
              возврат   │        │
                       ▼        ▼
                 ┌── @OnMethodExit адвайс ───┐
                 │   when == true ?          │
                 │   action.execute(         │
                 │      target, method, args,│
                 │      returnValue, thrown) │
                 └───────────────────────────┘
```

Если действие не должно выполняться для конкретного вызова, `when` вернёт `false` — адвайс просто ничего не сделает.

### Многопоточность

* Действие вызывается в потоке, который выполняет метод приложения.
* Экземпляр действия один на аннотацию — состояние должно быть потокобезопасным.
* Не блокируйте поток приложения надолго: действие выполняется синхронно внутри метода.

### Цепочки вызовов

* Если один конструктор вызывает другой через `this(...)`, enter-хук сработает на **каждом** конструкторе цепочки.
  Учитывайте это при подсчёте событий.
* Вложенные вызовы инструментируемых методов дают вложенные события `enter/exit` (как стек).

---

## Рецепты

### 1. Запись вызовов для проверок

Соберите вызовы в статический потокобезопасный коллектор и сравнивайте его содержимое в сценарии:

```java
public final class CallRecorder {
    private static final List<String> calls = Collections.synchronizedList(new ArrayList<>());

    public static void add(String call) {
        calls.add(call);
    }

    public static List<String> drain() {
        synchronized (calls) {
            List<String> copy = new ArrayList<>(calls);
            calls.clear();
            return copy;
        }
    }
}

public class RecordCallAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        CallRecorder.add(method.getName() + "(" + Arrays.toString(args) + ")");
    }
}
```

### 2. Условный хук

```java

@OnMethodEnter(
        pointcut = "t.inherits('com.example.worker.BaseWorker') && m.name('process')",
        when = "o.isSameClass(com.example.worker.CriticalWorker.class)"
                + " && a.arg(0).asString().startsWith('test-')",
        action = CriticalProcessAction.class)
public class CriticalProcessHookTest {
}
```

### 3. Замер длительности

Для пары enter/exit удобно использовать два действия и `ThreadLocal`:

```java
public class StartTimerAction implements MethodAction {
    static final ThreadLocal<Long> STARTED = new ThreadLocal<>();

    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        STARTED.set(System.nanoTime());
    }
}

public class StopTimerAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        Long started = StartTimerAction.STARTED.get();
        if (started != null) {
            CallRecorder.add(method.getName() + " took "
                    + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms");
        }
    }
}
```

### 4. Конструктор и статический метод

```java

@OnMethodEnter(pointcut = "t.name('com.example.Order') && m.isConstructor()",
        action = OrderCreatedAction.class)
@OnMethodEnter(pointcut = "t.name('com.example.Metrics') && m.name('reset')",
        action = MetricsResetAction.class)
public class ConstructorAndStaticHookTest {
}
```

В enter-хуке конструктора `target == null` (объект ещё не создан), в действии для static-метода — тоже `null`.

### 5. Реакция на исключение

Exit-хук получает исключение в параметре `thrown`:

```java
public class FailureAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args,
                        Object returnValue, Throwable thrown) {
        if (thrown != null) {
            CallRecorder.add(method.getName() + " failed: "
                    + thrown.getClass().getSimpleName() + ": " + thrown.getMessage());
        }
    }
}
```

### 6. Преобразование аргументов

`args` — это реальный массив, переданный в метод: изменив элемент, вы измените то, что увидит тело метода. Например,
можно нормализовать идентификатор или подменить значение в тестовом режиме. Помните, что это меняет поведение SUT,
поэтому применяйте осознанно.

### 7. Совместно с Wait-Completion

Хуки и стратегии ожидания — независимые плагины, их можно объявлять на одном классе:

```java

@FutureLikeAwait(pointcut = "t.name('com.example.AsyncClient') && m.name('send')",
        await = "o.call('get')")
@OnMethodEnter(pointcut = "t.name('com.example.AsyncClient') && m.name('send')",
        action = RecordAsyncCallAction.class)
class AsyncClientTest {
    // ...
}
```

---

## Сравнение механизмов расширения

| Критерий                        | `BeforeTest` / `AfterTest` / `TestConverter` | `@OnMethodEnter` / `@OnMethodExit`  | Wait-стратегии                        |
|---------------------------------|----------------------------------------------|-------------------------------------|---------------------------------------|
| Где объявляется                 | Spring-бины конфигурации                     | аннотации на тестовом классе        | аннотации на тестовом классе          |
| Область действия                | каждый тест (Container/Case/Part)            | каждый вызов подходящего метода SUT | фоновые задачи, попавшие под pointcut |
| Момент срабатывания             | вокруг теста                                 | при входе/выходе метода             | при проверке, блокирует тест          |
| Может менять данные SUT         | нет (данные теста)                           | да (через аргументы, target)        | нет                                   |
| Нужен доступ к Spring-бинам     | да (бин)                                     | нет (не бин)                        | нет                                   |
| Чувствителен к состоянию класса | нет                                          | да (`when` на каждый вызов)         | да                                    |
| Типичное применение             | setup/cleanup, нормализация                  | запись, проверки, замеры, эмуляция  | ожидание завершения                   |

---

## Устранение неполадок

### Хук не срабатывает

1. **Класс был загружен до установки агента.** Добавьте на тестовый класс
   `@RepeatInstrumentation({"com.example"})` — JUnit-расширение повторно трансформирует все загруженные классы с этим
   префиксом имени.
2. **Pointcut не совпал.** Проверьте имя класса (FQCN), метод и модификаторы; для унаследованных методов используйте
   `t.inherits(...)`. Ошибка компиляции `pointcut`/`when` (Janino) выбрасывается при настройке инструментации — см.
   лог запуска.
3. **Метод не имеет байт-кода.** `abstract` и `native` методы инструментировать нельзя.
4. **Это конструктор, а хук — exit.** На конструкторы ставится только `@OnMethodEnter`.
5. **Пустой фильтр метода.** Если в `pointcut` нет условий `m.*`, инструментируются все методы класса — убедитесь, что
   среди них есть целевой, и что лишние вызовы вам не мешают.

### Spring-прокси и имя класса

Если бин обёрнут CGLIB-прокси, имя класса в рантайме отличается от имени вашего класса. Используйте
`t.inherits('com.example.MyService')` или точное имя класса реализации и проверяйте, что хук не срабатывает дважды
(на прокси и на целевом объекте).

### Хук сработал дважды

* конструктор делегирует другому конструктору через `this(...)` — адвайс срабатывает на каждом;
* аннотация `@Inherited` + несколько классов в иерархии — проверьте, не объявлен ли хук дважды;
* пустой фильтр метода — под pointcut попало больше методов, чем ожидалось.

### Действие «молчит»

Проверьте `when`: возможно, выражение вернуло `false`. Если действие бросило исключение, оно записано в лог
`MethodHookAdvice` на уровне `FINE` — включите его, чтобы увидеть причину.

### Ошибка в `when` ломает бизнес-вызов

`when` вычисляется вне `try/catch` и при исключении пробрасывает его в метод приложения. Упростите выражение,
защищайте доступ к аргументам через `a.size()`/`o.isNull()` вместо прямых приведений.

---

## Ограничения и рекомендации

* **Exit-хук и конструкторы несовместимы** — используйте enter.
* **Хуки не ждут** — для синхронизации с асинхронностью применяйте [Wait-Completion](wait-completion.md).
* **Действие — не Spring-бин**: не рассчитывайте на инъекцию зависимостей.
* **Минимум логики в действии**: никаких долгих операций, сети и БД — вы работаете в потоке приложения.
* **Точные pointcut**: всегда фильтруйте метод, чтобы не инструментировать весь класс.
* **Потокобезопасность**: одно действие — много потоков.
* **Порядок нескольких хуков** не гарантирован; для строгой последовательности используйте одно действие.
* **Ошибки в действиях не видны по умолчанию** (уровень `FINE`) — если они важны, фиксируйте их в своих структурах и
  проверяйте в сценариях.

---

## Живой пример

Полный рабочий пример со всеми случаями (enter/exit, `when`, конструктор, static, исключение) есть в тестах модуля:

* `integration-testing/src/test/java/io/github/dimkich/integration/testing/execution/hook/HookTest.java`
* `integration-testing/src/test/java/io/github/dimkich/integration/testing/execution/hook/EnterAction.java`
* `integration-testing/src/test/java/io/github/dimkich/integration/testing/execution/hook/ExitAction.java`
* `integration-testing/src/test/resources/execution/hook/hook.xml`

---

## См. также

* [DSL выражений](Expression-DSL.md) — синтаксис и справочник `pointcut`/`when`.
* [Система Wait-Completion](wait-completion.md) — стратегии ожидания, использующие общий DSL.
* [Хуки и преобразователи](Hooks-and-Converters.md) — lifecycle-хуки теста (`BeforeTest`, `AfterTest`,
  `TestConverter`).
* [Сценарии с ошибками в Kafka](kafka/Error-Handling.md) — принудительный коммит незакоммиченного
  offset'а через exit-хук.
* [Инициализация тестов](Initialization.md) — настройка окружения перед сценариями.
