# Документация Integration Testing Framework

[English Version](../en/README.md)

Фреймворк для глубокого интеграционного тестирования Spring Boot приложений, позволяющий декларативно описывать сценарии
в XML/JSON и полностью контролировать окружение (БД, Время, Асинхронность).

## Основные возможности

* **Декларативность**: Описание тестов во внешних файлах без перекомпиляции кода.
* **Управление состоянием**: Автоматическое отслеживание «грязных» таблиц БД и интеллектуальная очистка.
* **Синхронизация асинхронности**: 4 стратегии ожидания завершения фоновых задач (Wait-Completion).
* **Манипуляция временем**: Полная подмена системного времени во всей JVM через `@MockJavaTime`.
* **Продвинутые моки**: Поддержка статических методов, конструкторов и запись реальных вызовов.

## Разделы документации

### Основы

* **[Быстрый старт](Quick-Start.md)** — Запуск первого теста за 5 минут.
* **[Иерархия тестов](Test-Hierarchy.md)** — Структура Container, Case, Part и правила выполнения (bean/method или
  inboundMessage).
* **[Конфигурация TestSetupModule](TestSetupModule.md)** — Регистрация типов, алиасов и настройка клонирования.
* **[Инициализация тестов](Initialization.md)** — Настройка окружения: время, БД, кэши, моки.

### Сериализация (Serde)

* **[Обзор Serde](serde/README.md)** — Выбор источника, `target-class`/`object-mapper-ref`, ядровые провайдеры,
  наследование настроек, диагностика.
* **[Бинарные упаковки](serde/Binary-Envelopes.md)** — Envelope-структура,
  теги `{CONTENT}`, `{LEN}`, `{VER}`, `{TS}`, `{CRC32}`, `{FIX}`, `{STR}`, примеры для Redis и Kafka.
* **[Расширяемость Serde](serde/Extensibility.md)** — Свои форматы (`TestSerdeProviderFactory`), фабрики
  конвертеров (`TestSerdeConverterFactory`),
  адаптеры (`TestSerdeAdapter`), декораторы (`TestSerdeDecoratorFactory`), теги `BinarySegment`.

### Redis

* **[Обзор Redis](redis/README.md)** — Настройка `@EnableTestRedis`, XML-примеры, типы данных.
* **[Типы данных](redis/Data-Types.md)** — Инициализация, `dataStorageDiff`, XML-шаблоны для Hash, List, Set, ZSet,
  Stream, HyperLogLog.
* **[Конфигурация](redis/Configuration.md)** — Подключения, Deep Merge, Longest-Prefix Match,
  полный `application-test.yml`.
* **[TTL и Time Shift](redis/TTL-Time-Shift.md)** — Виртуальное время, TTL хэш-полей (Redis 7.4+), пошаговый сценарий.
* **[Устранение неполадок](redis/Troubleshooting.md)** — `Method not found`, `No handler`, `Redis Sync Timeout`,
  пустой `dataStorageDiff`.
* **[Расширяемость](redis/Extensibility.md)** — Кастомные кодек/схема/доступ к данным, обработчики
  потоков и событий, `TestSetupModule.addSubTypes()`.

### Kafka

* **[Обзор Kafka](kafka/README.md)** — Настройка `@EnableTestKafka`, XML-примеры, отправка и проверка сообщений.
* **[Конфигурация](kafka/Configuration.md)** — Подключения, топики и regex-маски, настройки serde,
  `ignore`/`ignore-inbound`, `excluded-fields`.
* **[Записи и заголовки](kafka/Records-and-Headers.md)** — `<inboundMessage>`/`<outboundMessage>`, заголовки,
  tombstone, ошибки десериализации.
* **[Сериализация (Serde в Kafka)](kafka/Serde.md)** — Настройка записи целиком и по частям,
  `spring-json`/`spring-xml`, заголовки с информацией о типе; общие правила — в [обзоре Serde](serde/README.md).
* **[In-Flight и лаг](kafka/In-Flight-and-Lag.md)** — Ожидание обработки, два режима, таймауты, Cold Start, транзакции.
* **[Устранение неполадок](kafka/Troubleshooting.md)** — Timeout ожидания, ошибки serde-конфига, пропавшие
  сообщения, Docker.
* **[Сценарии с ошибками](kafka/Error-Handling.md)** — Незакоммиченные offset'ы, пропущенные упавшие сообщения,
  принудительный коммит из теста.
* **[Расширяемость](kafka/Extensibility.md)** — Свои сериализаторы, десериализаторы и заголовки через
  `bean-ref`; свои форматы — в [расширяемости Serde](serde/Extensibility.md).

### Инструменты и Интеграция

* **[Интеграция с IntelliJ IDEA](IDEA-Plugin.md)** — **(New!)** Управление тестами через плагин, визуальный Diff и режим
  Live Coding.
* **[Моки в тестах](Mocks.md)** — Использование `@TestBeanMock`, `@TestConstructorMock` и `@TestStaticMock`.
* **[Мокирование времени](MockJavaTime.md)** — Детерминированное время через `@MockJavaTime`.

### Расширенные возможности

* **[Система Wait-Completion](wait-completion.md)** — Ожидание завершения асинхронных процессов.
* **[Хуки на методы](Method-Hooks.md)** — `@OnMethodEnter`/`@OnMethodExit`: наблюдение и внедрение поведения в методы
  приложения.
* **[Хуки и преобразователи](Hooks-and-Converters.md)** — Использование `BeforeTest`, `AfterTest` и `TestConverter`.
* **[Аннотация JsonMapAsEntries](JsonMapAsEntries.md)** — Продвинутый маппинг коллекций для XML.
* **[Механизм ResettableIterator](ResettableIterator.md)** — Многоразовые итераторы для тестов.

---
[← На главную](../../README.md)