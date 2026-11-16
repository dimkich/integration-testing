Данные и diff
=============

[English version](../../en/postgresql/Data-and-Diff.md)

Тест с PostgreSQL обычно состоит из двух частей:

1. **Подготовка.** В `<init>` вы создаёте схему, загружаете DBUnit-датасет и при необходимости
   выполняете SQL.
2. **Проверка.** В `<diff>` вы описываете, какие строки и колонки должны измениться после
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

| Атрибут/элемент | Описание                                                        |
|-----------------|-----------------------------------------------------------------|
| `name`          | Имя подключения из `integration.testing.postgresql.connections` |
| `sqlFilePath`   | Пути к SQL-файлам на classpath; выполняются по порядку          |
| `sql`           | Inline SQL-запросы; выполняются после `sqlFilePath`             |
| `dbUnitPath`    | Пути к DBUnit-датасетам на classpath                            |

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

## `<diff>`

`<diff>` описывает изменения после выполнения теста. Атрибут `storage` задаёт имя хранилища
(`storage="dataSource"`), внутри — записи: строки таблиц. Запись всегда имеет один атрибут `key`,
а операция задаёт тег элемента: `insert`, `update` или `delete`.

Поля записи выводятся в порядке колонок таблицы (`CREATE TABLE` или `ALTER TABLE ... ADD COLUMN`),
а не по алфавиту.

Остальные правила — общие для всех хранилищ, они собраны в разделе
[Изменения хранилищ](../Storage-Diff.md). Особенности PostgreSQL: `hstore` выводится по алфавиту
(PostgreSQL не гарантирует порядок его полей), `json` — в порядке документа, `jsonb` — в том
порядке, в котором его вернул PostgreSQL.

### Вставка

При вставке показывается вся новая строка:

```xml
<diff storage="dataSource">
    <insert key="users/100">
        <id type="Integer">100</id>
        <name>Ivan</name>
        <email>ivan@example.com</email>
    </insert>
</diff>
```

### Обновление

При обновлении показываются только изменённые колонки:

```xml
<diff storage="dataSource">
    <update key="users/1">
        <name>Ivan Updated</name>
    </update>
</diff>
```

### Удаление

У удалённой строки тела нет, только ключ:

```xml
<diff storage="dataSource">
    <delete key="users/1"/>
</diff>
```

JSONB и hstore диффятся по ключам, события и вложенные объекты — по полям. Та же логика
работает на любой глубине: изменившееся поле — тег или `update key`, добавленное — `insert key`,
удалённое — `delete key`.

### Ключи строк

Если у таблицы есть первичный ключ, ключ строки имеет вид `[schema.]table/значение`:

```text
users/1
orders/42
billing.invoices/7
```

Для составного первичного ключа значения добавляются через запятую:

```text
orders/1,2
```

Часть ключа, относящаяся к таблице, зависит от `defaultSchema`: таблица из `defaultSchema`
записывается без схемы (`users/1`), таблица из другой схемы — с префиксом
(`billing.invoices/7`). Неквалифицированные имена в конфигурации и фикстурах разрешаются
в `defaultSchema`; для таблицы из другой схемы используйте `schema.table`.

Если первичного ключа нет, строка идентифицируется первыми 16 hex-символами хеша содержимого:

```text
nokey_data/20e4a4ea34a19357
```

В таблице без первичного ключа обновление выглядит как удаление старой строки и добавление
новой. Если такие diff'ы неудобны, добавьте первичный ключ.

Символы `/`, `,` и `%` внутри частей ключа экранируются как `%2F`, `%2C` и `%25`, поэтому
ключ всегда однозначно разбирается.

### Несколько частей теста

Внутри одного кейса `<diff>` можно указывать у каждой `<test type="Part">`. Diff каждой
части считается относительно состояния после предыдущей части:

```xml
<test type="Case" name="Accumulated changes between parts">
    <init type="SqlStorageInit" name="dataSource">
        <tablesToClear>users</tablesToClear>
    </init>

    <test type="Part" name="Insert">
        <!-- ... -->
        <diff storage="dataSource">... insert ...</diff>
    </test>

    <test type="Part" name="Update">
        <!-- ... -->
        <diff storage="dataSource">... update ...</diff>
    </test>

    <test type="Part" name="Delete">
        <!-- ... -->
        <diff storage="dataSource">... delete ...</diff>
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
