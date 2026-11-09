Данные и diff
=============

[English version](../../en/postgresql/Data-and-Diff.md)

Тест с PostgreSQL обычно состоит из двух частей:

1. **Подготовка.** В `<init>` вы создаёте схему, загружаете DBUnit-датасет и при необходимости
   выполняете SQL.
2. **Проверка.** В `<dataStorageDiff>` вы описываете, какие строки и колонки должны измениться после
   вызова кода приложения.

## SqlStorageSetup

`SqlStorageSetup` настраивает хранилище один раз на контейнер или тестовый класс. В нём выполняются
SQL-файлы, inline SQL и регистрируется DBUnit-датасет.

```xml
<init type="SqlStorageSetup" name="dataSource">
    <sqlFilePath>schema.sql</sqlFilePath>
    <sql>ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255)</sql>
    <dbUnitPath>db-data.xml</dbUnitPath>
</init>
```

| Элемент       | Описание                                                        |
|---------------|-----------------------------------------------------------------|
| `name`        | Имя подключения из `integration.testing.postgresql.connections` |
| `sqlFilePath` | Пути к SQL-файлам на classpath; выполняются по порядку          |
| `sql`         | Inline SQL-запросы; выполняются после `sqlFilePath`             |
| `dbUnitPath`  | Пути к DBUnit-датасетам на classpath                            |

`dbUnitPath` только регистрирует датасет. Данные из него загружаются позже через `SqlStorageInit`
с `tablesToLoad` или `loadAllTables="true"`.

DDL лучше выполнять именно в `SqlStorageSetup`: при изменении SQL модуль сбрасывает кэш метаданных
и заново читает структуру таблиц.

## SqlStorageInit

`SqlStorageInit` готовит состояние перед конкретным кейсом или частью теста.

```xml
<!-- Загрузить таблицу users из DBUnit-датасета -->
<init type="SqlStorageInit" name="dataSource">
    <tablesToLoad>users</tablesToLoad>
</init>

<!-- Очистить orders и загрузить users -->
<init type="SqlStorageInit" name="dataSource">
    <tablesToClear>orders</tablesToClear>
    <tablesToLoad>users</tablesToLoad>
</init>

<!-- Загрузить все таблицы датасета -->
<init type="SqlStorageInit" name="dataSource" loadAllTables="true"/>

<!-- Выполнить SQL после загрузки данных -->
<init type="SqlStorageInit" name="dataSource">
    <tablesToLoad>users</tablesToLoad>
    <sql>UPDATE users SET name = 'Init' WHERE id = 1</sql>
</init>
```

| Атрибут/элемент  | Описание                                            |
|------------------|-----------------------------------------------------|
| `name`           | Имя подключения                                     |
| `tablesToClear`  | Таблицы, которые нужно очистить                     |
| `tablesToLoad`   | Таблицы, которые нужно загрузить из DBUnit-датасета |
| `loadAllTables`  | `true` — загрузить все таблицы из датасета          |
| `sql`            | SQL-запросы, выполняемые после очистки и загрузки   |

Если таблица указана и в `tablesToClear`, и в `tablesToLoad`, она будет загружена из датасета.
`tablesToClear` без `tablesToLoad` оставляет таблицу пустой.

Список таблиц можно указывать через запятую. Полные имена (`public.users`) нужны, если таблицы с
одинаковым именем есть в разных схемах.

## DBUnit-датасет

DBUnit-файл — обычный XML-документ с корневым тегом `<dataset>`. Каждая таблица — отдельный тег,
каждая строка — повторение этого тега:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataset>
    <users>
        <id>1</id>
        <name>Alice</name>
        <email>alice@example.com</email>
    </users>
    <users>
        <id>2</id>
        <name>Bob</name>
        <email>bob@example.com</email>
    </users>

    <documents>
        <id>100</id>
        <status>NEW</status>
        <!-- NULL в БД; пустой тег означал бы пустую строку. -->
        <content null="true"/>
    </documents>
</dataset>
```

Значения колонок записываются как PostgreSQL-литералы:

```xml
<dataset>
    <!-- Массив -->
    <advanced>
        <id>500</id>
        <tags>{tag_x,tag_y}</tags>
    </advanced>

    <!-- JSON -->
    <advanced>
        <id>501</id>
        <data>{"version": 5, "active": true}</data>
    </advanced>

    <!-- hstore -->
    <datatypes>
        <id>1</id>
        <hs>"k1"=>"v1, with comma", "k2"=>"a:b:c"</hs>
    </datatypes>

    <!-- timestamptz с явным смещением -->
    <datatypes>
        <id>2</id>
        <ts_tz>2026-01-01 10:15:30.123456+03:00</ts_tz>
    </datatypes>
</dataset>
```

Особенности:

* `null="true"` задаёт SQL `NULL`; пустой тег — пустую строку.
* Порядок полей в строке не важен. Колонки таблицы — объединение всех полей по всем строкам
  в порядке первого появления; пропущенное в строке поле равно `NULL`.
* Дубликат поля в одной строке — ошибка.
* Массивы задаются текстовым литералом PostgreSQL, например `{a,b}`. Элементы могут содержать
  запятые, кавычки и `NULL`.
* `json`/`jsonb` и `hstore` передаются как текст и разбираются PostgreSQL.
* `bit`/`varbit` задаются строкой из `0` и `1`.
* `timestamptz`/`timetz` лучше задавать с явным смещением, чтобы результат не зависел от таймзоны JVM.
* Identity-колонки обычно не указываются: PostgreSQL сгенерирует значение сам.
* Для `hstore` нужно установить расширение `hstore`.

## dataStorageDiff

`<dataStorageDiff>` описывает изменения после выполнения теста. Diff строится по строкам и содержит
только изменившиеся колонки.

### Вставка

При вставке показывается вся новая строка:

```xml
<dataStorageDiff type="MapStringKeyObjectValue">
    <dataSource type="EntriesObjectKeyObjectValue">
        <entry change="added">
            <key>users_100</key>
            <value type="MapStringKeyObjectValue">
                <id type="Long">100</id>
                <name>Ivan</name>
                <email>ivan@example.com</email>
            </value>
        </entry>
    </dataSource>
</dataStorageDiff>
```

### Обновление

При обновлении показываются только изменённые колонки:

```xml
<dataStorageDiff type="MapStringKeyObjectValue">
    <dataSource type="EntriesObjectKeyObjectValue">
        <entry change="changed">
            <key>users_1</key>
            <value type="MapStringKeyObjectValue">
                <name>Ivan Updated</name>
            </value>
        </entry>
    </dataSource>
</dataStorageDiff>
```

### Удаление

У удалённой строки значение обязательно `nil`:

```xml
<dataStorageDiff type="MapStringKeyObjectValue">
    <dataSource type="EntriesObjectKeyObjectValue">
        <entry change="deleted">
            <key>users_1</key>
            <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        </entry>
    </dataSource>
</dataStorageDiff>
```

### Ключи строк

Если у таблицы есть первичный ключ, ключ строки имеет вид:

```text
<таблица>_<значение первичного ключа>
```

Примеры:

```text
users_1
orders_42
billing.invoices_7
```

Для составного первичного ключа значения добавляются через `_`. Для читаемости лучше использовать
простые первичные ключи.

Часть ключа, относящаяся к таблице, зависит от `defaultSchema`: таблица из `defaultSchema`
записывается без схемы (`users_1`), таблица из другой схемы — с префиксом (`billing.invoices_7`).
Неквалифицированные имена в конфигурации и фикстурах разрешаются в `defaultSchema`; для таблицы
из другой схемы используйте `schema.table`.

Если первичного ключа нет, строка идентифицируется хешем всего содержимого:

```text
nokey_data_20e4a4ea34a193578fdcbb989221baab40a137646521718fbe55d9df70db52ad
```

В таблице без первичного ключа обновление выглядит как удаление старой строки и добавление новой.
Если такие diff'ы неудобны, добавьте первичный ключ.

### Несколько частей теста

Внутри одного кейса `<dataStorageDiff>` можно указывать у каждой `<test type="Part">`. Diff каждой
части считается относительно состояния после предыдущей части:

```xml
<test type="Case" name="Accumulated changes between parts">
    <init type="SqlStorageInit" name="dataSource">
        <tablesToClear>users</tablesToClear>
    </init>

    <test type="Part" name="Insert">
        <!-- ... -->
        <dataStorageDiff>... added ...</dataStorageDiff>
    </test>

    <test type="Part" name="Update">
        <!-- ... -->
        <dataStorageDiff>... changed ...</dataStorageDiff>
    </test>

    <test type="Part" name="Delete">
        <!-- ... -->
        <dataStorageDiff>... deleted ...</dataStorageDiff>
    </test>
</test>
```

Если одна и та же строка несколько раз изменилась в пределах одной части, diff показывает
итоговое состояние относительно начала части. Вставка с последующим удалением не даёт записи в diff.

### Что не попадает в diff

* Колонки с `ignore: true`.
* Вложенные поля, перечисленные в `excludedFields`.
* Технические таблицы модуля.
* Изменения, которые не прошли через триггеры PostgreSQL (например, при
  `session_replication_role = 'replica'` или отключённых триггерах).

Сами значения колонок и настройки serde описаны в разделе [Типы и serde](Types-and-Serde.md).

---
[← На главную](../README.md)
