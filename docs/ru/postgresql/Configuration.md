Конфигурация модуля PostgreSQL
==============================

[English version](../../en/postgresql/Configuration.md)

Параметры модуля задаются в стандартных файлах Spring Boot (`application.yml`,
`application-test.yml` и т.п.) под префиксом `integration.testing.postgresql`.

Фреймворк открывает собственные JDBC-подключения по этим параметрам. Ему не нужен бин `DataSource`
из приложения, но приложение и тест должны смотреть в одну и ту же базу, иначе diff не увидит
изменения приложения.

## Подключения

Ключ в `connections` — это имя хранилища. Оно используется в XML:

```xml
<init type="SqlStorageSetup" name="dataSource">...</init>
<init type="SqlStorageInit" name="dataSource">...</init>
<dataStorageDiff>
    <dataSource>...</dataSource>
</dataStorageDiff>
```

Имя может быть любым, например `dataSource`, `orders`, `billing`. Оно не обязано совпадать с именем
бина `DataSource` в приложении.

### Глобальные параметры

Параметры задают значения по умолчанию для всех подключений:

| Параметр      | Тип    | Описание                          | Значение по умолчанию             |
|---------------|--------|-----------------------------------|-----------------------------------|
| `host`        | String | Хост PostgreSQL                   | `${embedded.postgresql.host}`     |
| `port`        | String | Порт PostgreSQL                   | `${embedded.postgresql.port}`     |
| `database`    | String | Имя базы данных                   | `${embedded.postgresql.schema}`   |
| `user`        | String | Пользователь                      | `${embedded.postgresql.user}`     |
| `password`    | String | Пароль                            | `${embedded.postgresql.password}` |
| `connections` | Map    | Подключения, ключ — имя хранилища | пусто                             |

У embedded-контейнера имя базы данных исторически доступно через свойство
`embedded.postgresql.schema`, поэтому в примерах `database` ссылается именно на него.

### Параметры подключения

Каждое подключение может переопределить глобальные значения:

| Параметр               | Тип    | Описание                                         | По умолчанию |
|------------------------|--------|--------------------------------------------------|--------------|
| `host`                 | String | Хост PostgreSQL                                  | глобальный   |
| `port`                 | String | Порт                                             | глобальный   |
| `database`             | String | Имя базы данных                                  | глобальный   |
| `user`                 | String | Пользователь                                     | глобальный   |
| `password`             | String | Пароль                                           | глобальный   |
| `defaultSchema`        | String | Схема для неквалифицированных имён и ключей diff | `public`     |
| `batchSize`            | int    | Размер JDBC-пакета при загрузке DBUnit-фикстуры  | `10000`      |
| `sequenceRestartValue` | long   | Значение `RESTART WITH` для sequence подключения | `1`          |
| `columns`              | Map    | Настройки отдельных колонок                      | пусто        |

`defaultSchema` влияет на разрешение имён `users`, `orders` и т.п. Если таблицы лежат в другой
схеме, укажите её в `defaultSchema` или используйте полные имена `schema.table`.

Это же значение определяет префикс таблицы в ключах строк `<dataStorageDiff>`: таблицы из
`defaultSchema` используют короткое имя (`users_1`), таблицы из других схем сохраняют схему
(`billing.users_1`).

Пример с двумя подключениями:

```yaml
integration:
  testing:
    postgresql:
      connections:
        dataSource:
          host: "${embedded.postgresql.host}"
          port: "${embedded.postgresql.port}"
          database: "${embedded.postgresql.schema}"
          user: "${embedded.postgresql.user}"
          password: "${embedded.postgresql.password}"
          defaultSchema: public

        billing:
          host: db.example.test
          port: 5432
          database: billing
          user: test_user
          password: test_password
          defaultSchema: billing
```

### Перезапуск последовательностей

При сбросе состояния вокруг границ тестов фреймворк выполняет для sequence управляемых таблиц:

```sql
ALTER SEQUENCE <sequence> RESTART WITH <sequenceRestartValue>
```

По умолчанию `sequenceRestartValue` равен `1`. Значение задаётся для всего подключения и
применяется ко всем его sequence.

Есть два типовых способа развести ID фикстур и ID, которые генерирует тестируемая система:

1. В фикстурах ID начинаются с `1000`, а SUT генерирует ID с `1`. Оставьте
   `sequenceRestartValue: 1`.
2. В фикстурах ID начинаются с `1`, а SUT должен генерировать ID с `1000`. Установите
   `sequenceRestartValue: 1000` (или больше).

```yaml
integration:
  testing:
    postgresql:
      connections:
        dataSource:
          # ...
          sequenceRestartValue: 1000
```

Значение должно быть больше максимального явного ID в фикстурах каждой таблицы. Иначе первый
`INSERT` тестируемой системы может получить уже занятый ID и завершиться ошибкой duplicate key.
Настройка также сдвигает генерируемые ID и ключи в diff, поэтому тесты с абсолютными ID нужно
обновить.

Если отдельной таблице нужно другое значение, переопределите его SQL-запросом в тесте после
автоматического сброса sequence.

## Форма diff в XML

Модуль складывает изменения в обычное хранилище фреймворка. Чтобы получить привычный вид
`MapStringKeyObjectValue` → `EntriesObjectKeyObjectValue` с атрибутом `change`, добавьте настройку:

```yaml
integration:
  testing:
    storage:
      storages:
        dataSource:
          keyType: [ STRING, OBJECT ]
          valueType: [ OBJECT, OBJECT ]
          changeType: [ false, true ]
```

Здесь уровни означают:

* первый элемент — корневой diff, ключ — имя подключения;
* второй элемент — записи внутри подключения, ключ — строка вида `users_100`.

`changeType` включает атрибут `change="added|changed|deleted"`. Для второго подключения добавьте
аналогичный блок `storages.billing`.

## Настройки колонок

Секция `columns` позволяет переопределить serde и исключить данные из diff. Ключ — это
`table.column` или `schema.table.column`.

В YAML ключ с точками обязательно берите в кавычки и квадратные скобки:

```yaml
integration:
  testing:
    postgresql:
      connections:
        dataSource:
          columns:
            "[public.users.metadata]":
              type: json
              target-class: com.example.UserMetadata
            "[public.users.technical_note]":
              ignore: true
            "[public.events.payload]":
              type: json
              excludedFields:
                - secret
                - metrics.internal
```

Поддерживаются поля:

| Поле                | Тип           | Назначение                                                |
|---------------------|---------------|-----------------------------------------------------------|
| `type`              | String        | Имя провайдера serde или полное имя класса                |
| `bean-ref`          | String        | Имя Spring-бина с готовым сериализатором/десериализатором |
| `target-class`      | String        | Класс значения для `json`/`xml`/`yaml`                    |
| `object-mapper-ref` | String        | Имя бина `ObjectMapper` для Jackson-форматов              |
| `binary-envelope`   | String        | Шаблон бинарного конверта                                 |
| `ignore`            | boolean       | Полностью исключить колонку из diff                       |
| `excludedFields`    | Set\<String\> | Вложенные поля, которые нужно исключить из diff           |

`ignore` исключает колонку только из сравнения. Аудит и откат по-прежнему работают с полным
значением, поэтому такие колонки корректно восстанавливаются.

`excludedFields` удаляет указанные вложенные поля из обоих состояний — «до» и «после». Пути
разделяются точками; правило применяется и к элементам JSON-массивов.

Колонки первичного ключа нельзя указывать в `ignore` или `excludedFields`: они участвуют в
идентификации строки, поэтому при подготовке metadata будет ошибка. Для таблицы без первичного
ключа идентификатором остаётся весь tuple: `ignore` скрывает значение из diff, но изменение
только такой колонки всё равно выглядит как удаление и добавление строки.

Поля serde наследуются от общей конфигурации. Подробности и примеры — в разделе
[Типы и serde](Types-and-Serde.md) и в [общем обзоре Serde](../serde/README.md).

## Требования к окружению

Для работы модуля техническая учётная запись PostgreSQL должна иметь возможность:

* подключаться к нужной базе;
* создавать в схеме служебную таблицу журнала, последовательность, функции и триггеры;
* создавать и удалять триггеры на тестируемых таблицах;
* выполнять `SET session_replication_role` (обычно это право есть только у суперпользователя или
  у роли, которой оно явно выдано);
* выполнять `ALTER SEQUENCE ... RESTART` для последовательностей тестируемых таблиц;
* читать и писать в служебную таблицу журнала.

Если в тестах используются колонки `hstore`, расширение должно быть установлено заранее:

```sql
CREATE EXTENSION IF NOT EXISTS hstore;
```

Модуль ожидает стандартный `bytea_output = 'hex'`. При `bytea_output = 'escape'` бинарные значения
могут отображаться как текст, а не как `byte[]`.

Служебные таблицы с префиксом `_it_` не предназначены для тестовых данных: модуль сам исключает их
из метаданных и фикстур. Не очищайте и не изменяйте их вручную.

По умолчанию `@EnableTestPostgreSQL` подключает `postgresql.properties` и включает embedded PostgreSQL
из Testcontainers. Для внешней базы отключите embedded-режим через свойства теста, например:

```java
@SpringBootTest(properties = {
        "embedded.containers.enabled=false",
        "embedded.postgresql.enabled=false"
})
```

## Полный пример

```yaml
integration:
  testing:
    postgresql:
      connections:
        dataSource:
          host: "${embedded.postgresql.host}"
          port: "${embedded.postgresql.port}"
          database: "${embedded.postgresql.schema}"
          user: "${embedded.postgresql.user}"
          password: "${embedded.postgresql.password}"
          defaultSchema: public
          batchSize: 5000
          columns:
            "[public.documents.content]":
              ignore: true
            "[public.events.payload]":
              type: json
              excludedFields:
                - secret
    storage:
      storages:
        dataSource:
          keyType: [ STRING, OBJECT ]
          valueType: [ OBJECT, OBJECT ]
          changeType: [ false, true ]
```

---
[← На главную](../README.md)
