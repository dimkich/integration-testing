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

### Redis

* **[Обзор Redis](redis/README.md)** — Настройка `@EnableTestRedis`, XML-примеры, типы данных.
* **[Типы данных](redis/Data-Types.md)** — Инициализация, `dataStorageDiff`, XML-шаблоны для Hash, List, Set, ZSet,
  Stream, HyperLogLog.
* **[Конфигурация](redis/Configuration.md)** — Подключения, Deep Merge, Longest-Prefix Match,
  полный `application-test.yml`.
* **[Бинарные упаковки](redis/Binary-Envelopes.md)** — Envelope-структура,
  теги `{CONTENT}`, `{LEN}`, `{VER}`, `{TS}`, `{CRC32}`, `{FIX}`, `{STR}`, кастомные теги.
* **[TTL и Time Shift](redis/TTL-Time-Shift.md)** — Виртуальное время, TTL хэш-полей (Redis 7.4+), пошаговый сценарий.
* **[Устранение неполадок](redis/Troubleshooting.md)** — `Method not found`, `No handler`, `Redis Sync Timeout`,
  пустой `dataStorageDiff`.
* **[Расширяемость](redis/Extensibility.md)** — Кастомные кодек/схема/доступ к данным, теги `BinarySegment`, обработчики
  потоков и событий, `TestSetupModule.addSubTypes()`.

### Инструменты и Интеграция

* **[Интеграция с IntelliJ IDEA](IDEA-Plugin.md)** — **(New!)** Управление тестами через плагин, визуальный Diff и режим
  Live Coding.
* **[Моки в тестах](Mocks.md)** — Использование `@TestBeanMock`, `@TestConstructorMock` и `@TestStaticMock`.
* **[Мокирование времени](MockJavaTime.md)** — Детерминированное время через `@MockJavaTime`.

### Расширенные возможности

* **[Система Wait-Completion](wait-completion.md)** — Ожидание завершения асинхронных процессов.
* **[Хуки и преобразователи](Hooks-and-Converters.md)** — Использование `BeforeTest`, `AfterTest` и `TestConverter`.
* **[Аннотация JsonMapAsEntries](JsonMapAsEntries.md)** — Продвинутый маппинг коллекций для XML.
* **[Механизм ResettableIterator](ResettableIterator.md)** — Многоразовые итераторы для тестов.

---
[← На главную](../../README.md)