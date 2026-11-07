Интеграционное тестирование PostgreSQL
=======================================

[English version](../../en/postgresql/README.md)

Модуль позволяет тестировать приложение, которое работает с PostgreSQL, не описывая проверки вручную.
Вы готовите данные перед тестом, вызываете код приложения и указываете в XML, какие изменения должны
произойти в базе. Фреймворк сам читает изменения, строит diff и восстанавливает состояние между тестами.

Модуль рассчитан на сценарии, где приложение и тест работают с одной и той же базой. Отдельный
`DataSource` приложения при этом не обязателен: фреймворк открывает собственное JDBC-подключение по
параметрам из конфигурации.

## Что умеет модуль

* Подготавливать схему и данные через SQL-файлы, inline SQL и DBUnit-датасеты.
* Автоматически показывать изменения строк в формате `dataStorageDiff`.
* Сравнивать состояние до и после теста, а не писать assert'ы для каждой колонки.
* Поддерживать PostgreSQL-типы: массивы, `json`/`jsonb`, `hstore`, `bytea`, составные типы, время,
  identity- и generated-колонки.
* Настраивать serde для отдельных колонок и исключать технические поля из diff.
* Возвращать базу к нужному состоянию перед следующим тестом без полной перезагрузки всех данных,
  когда это возможно.

## Как это работает

1. **Подготовка.** Перед тестом выполняются `SqlStorageSetup` и `SqlStorageInit`: создаётся схема,
   загружается DBUnit-датасет, применяются SQL-запросы.
2. **Выполнение.** Тест вызывает метод вашего приложения или отправляет сообщение. Изменения,
   которые приложение делает в PostgreSQL, попадают в технический журнал.
3. **Проверка.** После теста фреймворк читает журнал и сравнивает начальное и конечное состояние
   строк. Результат сравнения показывается в `<dataStorageDiff>`.
4. **Переход к следующему тесту.** Следующий `init` возвращает базу к своему состоянию. Если откат
   строк дешевле перезагрузки таблицы, фреймворк откатывает изменения; иначе перезагружает таблицу
   из DBUnit-датасета.

## Подключение

Добавьте зависимость модуля:

```xml
<dependency>
    <groupId>io.github.dimkich.integration.testing.postgresql</groupId>
    <artifactId>integration-testing-postgresql</artifactId>
</dependency>
```

Включите поддержку PostgreSQL одной аннотацией над тестовым классом:

```java
package com.example.postgres;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.postgresql.EnableTestPostgreSQL;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

@EnableTestPostgreSQL
@SpringBootTest
@ActiveProfiles("test")
public class UserPostgresTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("tests/postgres-user.xml");
    }
}
```

`@EnableTestPostgreSQL` подключает `postgresql.properties`, который по умолчанию включает
embedded PostgreSQL из Testcontainers. Для внешней базы значения подключения можно задать явно, а
embedded-режим отключить через свойства теста.

## Быстрый старт

### Конфигурация подключения

Имя `dataSource` в примере — это имя подключения фреймворка. Оно не обязано совпадать с именем
бина `DataSource` в приложении, но должно совпадать с `name` в `<init>` и в `<dataStorageDiff>`.

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
    storage:
      storages:
        dataSource:
          keyType: [ STRING, OBJECT ]
          valueType: [ OBJECT, OBJECT ]
          changeType: [ false, true ]
```

`integration.testing.storage.storages` задаёт форму XML-diff. Если она не нужна, этот блок можно
не указывать; подробности — в разделе [Конфигурация](Configuration.md).

### Файл тестов

```xml
<?xml version="1.0" encoding="UTF-8"?>
<test type="Container" name="Users">
    <!-- Схема и DBUnit-датасет регистрируются один раз на контейнер. -->
    <init type="SqlStorageSetup" name="dataSource">
        <sqlFilePath>schema.sql</sqlFilePath>
        <dbUnitPath>db-data.xml</dbUnitPath>
    </init>

    <test type="Case" name="Create user">
        <!-- Загружаем таблицу users из db-data.xml. -->
        <init type="SqlStorageInit" name="dataSource">
            <tablesToLoad>users</tablesToLoad>
        </init>

        <bean>userService</bean>
        <method>create</method>
        <request type="Long">100</request>
        <request>Ivan</request>
        <request>ivan@example.com</request>

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
    </test>
</test>
```

Формат `dataStorageDiff` и правила формирования ключей описаны в разделе
[Данные и diff](Data-and-Diff.md).

## Требования

* JDK 17 и Spring Boot 3.x.
* PostgreSQL. Для embedded-режима нужен запущенный Docker.
* Техническая учётная запись должна уметь создавать служебную таблицу журнала, функции и триггеры,
  а также выполнять `ALTER SEQUENCE` и `SET session_replication_role`. Подробности — в разделе
  [Конфигурация](Configuration.md#требования-к-окружению).
* Для колонок `hstore` должно быть установлено расширение `hstore`.
* Для `bytea` ожидается стандартный `bytea_output = 'hex'`.

## Дальше

* [Конфигурация](Configuration.md) — подключения, права, `columns`, `ignore` и `excludedFields`.
* [Данные и diff](Data-and-Diff.md) — `SqlStorageSetup`, `SqlStorageInit`, DBUnit и `dataStorageDiff`.
* [Типы и serde](Types-and-Serde.md) — как PostgreSQL-типы выглядят в XML и как настроить свои.
* [Устранение неполадок](Troubleshooting.md) — пустой diff, ошибки прав, конвертации и схемы.

---
[← На главную](../README.md)
