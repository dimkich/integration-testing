Документация подсистемы инициализации тестов
============================================

Обзор
-----

Подсистема инициализации тестов предоставляет механизм для настройки состояния окружения перед выполнением тестов. Она
позволяет декларативно конфигурировать различные аспекты тестовой среды через XML-файлы, включая время, данные в БД,
состояние бинов, стабы и хранилища ключ-значение.

### Ключевые возможности

| Возможность                    | Описание                                                         |
|--------------------------------|------------------------------------------------------------------|
| **Декларативная конфигурация** | Настройка инициализации через XML-элементы `<init>`              |
| **Иерархическое наследование** | Настройки наследуются сверху вниз по иерархии тестов             |
| **Модульная архитектура**      | Каждый тип инициализации реализуется через отдельный `InitSetup` |
| **Управление состоянием**      | Состояние инициализации сохраняется и сравнивается между тестами |
| **Приоритет выполнения**       | Каждый тип инициализации имеет порядок выполнения (`order`)      |

Базовый синтаксис
-----------------

Элемент `<init>` добавляется внутрь любого теста (`Container`, `Case`, `Part`):

```
xml
<test type="Container">
    <!-- Инициализация на уровне контейнера -->
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>

    <test type="Case" name="тест1">
        <!-- Наследует dateTimeInit от родителя + своя инициализация -->
        <init type="sqlStorageInit" name="dataSource">
            <tablesToClear>users</tablesToClear>
        </init>
        <bean>userService</bean>
        <method>create</method>
    </test>
</test>

```

### Атрибут `applyTo` (опционально)

Позволяет ограничить область применения инициализации:

| Значение        | Действие                                        |
|-----------------|-------------------------------------------------|
| Не указан       | Применяется ко всем типам тестов (по умолчанию) |
| `TestContainer` | Только к контейнерам                            |
| `TestCase`      | Только к тест-кейсам                            |
| `TestPart`      | Только к частям тестов                          |

**Пример использования:**

```
xml
<!-- Применится ко всем TestCase внутри этого контейнера одной записью -->
<test type="Container">
    <init type="StubInit" applyTo="TestCase" resetAll="true"/>

    <test type="Case" name="кейс1">
        <!-- StubInit применён автоматически -->
    </test>
    <test type="Case" name="кейс2">
        <!-- StubInit применён автоматически -->
    </test>
</test>

```

*** ** * ** ***

Типы инициализации с примерами
------------------------------

### 1. DateTimeInit --- Управление временем

**Назначение:** Установка фиксированного времени или сдвиг времени для детерминированных тестов.

#### Примеры

```
xml
<!-- Абсолютное время для всех тестов в контейнере -->
<test type="Container">
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>

    <test type="Case" name="фиксированноеВремя">
        <bean>dateTimeService</bean>
        <method>now</method>
        <response type="ZonedDateTime">2025-01-01T00:00:00Z</response>
    </test>
</test>

```

```
xml
<!-- Прогрессия времени внутри многошагового теста -->
<test type="Case" name="многошаговыйСценарий">
    <!-- Базовое время для кейса -->
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>
    <!-- Сдвиг на 1 минуту для каждого Part -->
    <init type="dateTimeInit" applyTo="TestPart" addDuration="PT1M"/>

    <test type="Part" name="шаг1">
        <!-- Время: 2025-01-01T00:01:00Z -->
        <response type="ZonedDateTime">2025-01-01T00:01:00Z</response>
    </test>
    <test type="Part" name="шаг2">
        <!-- Время: 2025-01-01T00:02:00Z -->
        <response type="ZonedDateTime">2025-01-01T00:02:00Z</response>
    </test>
</test>

```

#### Параметры

| Атрибут       | Описание                               | Пример                   |
|---------------|----------------------------------------|--------------------------|
| `dateTime`    | Абсолютное время (ISO-8601)            | `2025-01-01T00:00:00Z`   |
| `addDuration` | Длительность для добавления (ISO-8601) | `PT1M`, `PT2H30M`, `P1D` |

Оба атрибута можно указать в одном `<init>`: итоговое время будет `dateTime + addDuration`.
`dateTime` --- это жёсткая установка времени: он сбрасывает `addDuration`, накопленный до него в этом же или внешнем
скоупе. Поэтому при объявлении отдельными элементами `dateTime` должен идти раньше `addDuration`.

Для `addDuration` обязательна база. Если `dateTime` не задан в текущем или внешнем скоупе, при применении
инициализации будет ошибка. На практике это значит, что в каждом файле тестов, где используется `DateTimeInit`,
нужно задать абсолютный `dateTime` в корневом контейнере.

*** ** * ** ***

### 2. SqlStorageSetup --- Настройка SQL-хранилища

**Назначение:** Первоначальная конфигурация БД: выполнение SQL-скриптов, загрузка DBUnit-датасетов.

#### Примеры

```
xml
<!-- Базовая настройка с DBUnit -->
<init type="sqlStorageSetup" name="dataSource">
    <dbUnitPath>initializationData.xml</dbUnitPath>
</init>

```

```
xml
<!-- Настройка с SQL-файлами -->
<init type="sqlStorageSetup" name="dataSource">
    <sqlFilePath>schema.sql</sqlFilePath>
    <sqlFilePath>test-data.sql</sqlFilePath>
    <dbUnitPath>users.xml</dbUnitPath>
</init>

```

```
xml
<!-- Настройка с inline SQL -->
<init type="sqlStorageSetup" name="dataSource">
    <sql>CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(100))</sql>
    <sql>INSERT INTO users VALUES (1, 'John')</sql>
    <dbUnitPath>users.xml</dbUnitPath>
</init>

```

#### Параметры

| Элемент       | Описание                                                  |
|---------------|-----------------------------------------------------------|
| `name`        | Имя хранилища (совпадает с бином `SQLDataStorageService`) |
| `sqlFilePath` | Пути к SQL-файлам (classpath)                             |
| `sql`         | Inline SQL-запросы                                        |
| `dbUnitPath`  | Пути к DBUnit XML-датасетам                               |

*** ** * ** ***

### 3. SqlStorageInit --- Инициализация таблиц

**Назначение:** Очистка и загрузка данных таблиц **перед конкретным тестом** .

#### Примеры

```
xml
<!-- Очистить конкретные таблицы -->
<init type="sqlStorageInit" name="dataSource">
    <tablesToClear>t1,t2</tablesToClear>
</init>

```

```
xml
<!-- Загрузить все таблицы из DBUnit -->
<init type="sqlStorageInit" name="dataSource" loadAllTables="true"/>

```

```
xml
<!-- Очистить и загрузить конкретные таблицы -->
<init type="sqlStorageInit" name="dataSource">
    <tablesToClear>t1</tablesToClear>
    <tablesToLoad>t1</tablesToLoad>
</init>

```

```
xml
<!-- Выполнить SQL-запросы -->
<init type="sqlStorageInit" name="dataSource">
    <sql>update t1 set column = 1</sql>
</init>

```

#### Параметры

| Атрибут         | Описание                                            |
|-----------------|-----------------------------------------------------|
| `name`          | Имя хранилища                                       |
| `tablesToClear` | Список таблиц, которые очищаются (данные удаляются) |
| `tablesToLoad`  | Список таблиц для загрузки данных из DBUnit         |
| `loadAllTables` | `true` --- загрузить все таблицы из датасета        |
| `sql`           | SQL-запросы для выполнения                          |

#### История состояний и откат

Каждая инициализация добавляет в состояние хранилища упорядоченный `MutationLog`: объявления
baseline `LOAD`/`CLEAR`, SQL-мутации и изменения, которые тестируемая система вносит во время
теста. Статусы таблиц хранятся отдельно в `TableBaseline`. Каждая `StorageMutation` несёт
затронутые таблицы и команды отката по каждой таблице. Привязка мутаций к таблицам строится один
раз --- когда после теста читается журнал изменений (единственное чтение за тест, см. ниже):
записи без тега --- это изменения тестируемой системы, записи с тегом --- SQL применённых мутаций.
Таблицы, которые изменяет блок `<sql>`, **не парсятся** из текста запроса: их определяет
содержимое журнала.

Перед новым тестом `StorageState.planTransition` вычисляет `TransitionPlan` из исходного и
целевого состояний. `TransitionAnalyzer` и `TableTransition` находят структурный общий префикс,
сравнивают baseline, оценивают возможность и стоимость отката каждой таблицы и каскадно разрешают
необходимые базовые сбросы. Мутация, общая для нескольких таблиц, считается сохраняемой только
если она общая для всех них; иначе все её таблицы откатываются до этой мутации и она накатывается
ровно один раз. Так общий SQL не откатывается и не переисполняется для таблицы, ветка которой не
менялась. Откат **никогда не проходит сквозь изменение baseline**: таблица, к которой после
общего префикса применялся `LOAD`/`CLEAR`, не откатывается построчно, а перезагружается или
очищается. План содержит только команды, которые действительно меняют данные: пустой откат не
инвалидирует таблицу.

Применение перехода состоит из четырёх последовательных фаз:

1. untagged-откат старого хвоста и очистка сбрасываемых таблиц;
2. загрузка целевых фикстур DBUnit;
3. untagged-перезапуск сиквенсов;
4. tagged-накат целевых SQL-мутаций.

Тег кладётся в сессионную переменную `it.node` и попадает в колонку `node` журнала
`_it_postgres_journal_<random>` --- по нему следующее чтение атрибутирует записи мутациям.

Для delta-хранилища на триггерах откат исполняет готовые команды мутаций: если откат дешевле
перезагрузки фикстуры (суммарное число записей аудита по мутациям хвоста меньше размера фикстуры
плюс один) --- команды исполняются; иначе --- если изменение откатить нельзя или хвост пересёк
границу `LOAD`/`CLEAR` --- таблица перезагружается из DBUnit-датасета или очищается по своему
baseline. Команды отката строятся множественно (записи сквошатся по
первичному ключу), поэтому их число пропорционально видам изменений, а не числу записей.
Генерируемые колонки не вынуждают перезагрузку: колонки `GENERATED ALWAYS AS (...) STORED`
исключаются из обратных `INSERT`/`UPDATE` (PostgreSQL пересчитывает их из восстановленных базовых
колонок), а значения `GENERATED ALWAYS AS IDENTITY` восстанавливаются через `OVERRIDING SYSTEM VALUE`
(если значение identity действительно перегенерировалось, строка удаляется и вставляется заново).
Хранилища без аудит-лога используют обычный исполнитель по разнице таблиц.

#### Синхронизация кэша таблиц

Кэш приложения моделируется через монотонные ревизии таблиц. Для каждой таблицы фреймворк ведёт
две ревизии: ревизию данных в БД, которая растёт при изменении со стороны тестового фреймворка
(`LOAD`/`CLEAR`, `<sql>`, непустой откат или `clearTables`), и ревизию, которую подтвердил кэш
приложения. Пока ревизия БД больше, кэш считается устаревшим. Синхронизацию выполняет
`OnTableChangeInit` (см. ниже): для каждой устаревшей таблицы из своей области видимости он вызывает
настроенное действие --- **только вызов метода бина, без запросов в БД** --- и подтверждает ревизию.

Изменения, которые тестируемая система вносит во время теста, фиксируются в журнале мутаций
(вместе со своими обратными командами), но не двигают ревизию БД: SUT отслеживает свои изменения
сам. Поэтому такое изменение не подтверждает более раннее изменение фреймворка, и устаревший кэш
остаётся устаревшим до реальной синхронизации действием. Если следующий тест откатывает изменения
SUT, этот откат является изменением фреймворка и действие вызывается снова.

*** ** * ** ***

### 4. OnTableChangeInit --- Синхронизация кэша таблиц

**Назначение:** Вызвать настроенные методы бинов для таблиц, чья ревизия БД больше подтверждённой
ревизии кэша. Это **действие**, а не состояние: оно выполняется каждый раз, когда попадает в
область видимости, и ничего не делает, если устаревших таблиц нет.

#### Примеры

```
xml
<!-- Один раз на контейнер: для всех TestCase обновлять кэш users и orders -->
<init type="OnTableChangeInit" name="dataSource" applyTo="TestCase">
    <onTableChange tableName="users" beanName="cacheService" beanMethod="reloadUsers"/>
    <onTableChange tableName="orders" beanName="cacheService" beanMethod="reloadOrders"/>
</init>

```

```
xml
<!-- Отменить действия, унаследованные из контейнера, для одного теста -->
<init type="OnTableChangeInit" name="dataSource"/>

```

#### Параметры

| Атрибут/Элемент | Описание                                                                                       |
|-----------------|------------------------------------------------------------------------------------------------|
| `name`          | Имя хранилища                                                                                  |
| `applyTo`       | Уровень раскрытия действия (`TestContainer`/`TestCase`/`TestPart`)                             |
| `onTableChange` | Запись `tableName` + `beanName` + `beanMethod`; пустой список отменяет унаследованные действия |

#### Область видимости и отмена

Действие раскрывается через `applyTo` и наследуется вниз по дереву. Более вложенное объявление
заменяет унаследованное **для своего хранилища** (`name`): список `onTableChange` перекрывается целиком,
поэтому `OnTableChangeInit` с пустым списком отключает действия для своего поддерева. Разные
хранилища не конфликтуют --- каждое перекрывается своим `name`. Если в поддереве есть и `TestCase`,
и `TestPart`, нужно объявить действие для обоих уровней (`applyTo="TestCase"` и
`applyTo="TestPart"`).

Действия вызываются для таблиц, у которых кэш устарел (см. «Синхронизация кэша таблиц»); если
данные не менялись, действий не будет. Инициализация выполняется после `SqlStorageInit` и
`BeanInit`, поэтому видит данные всех хранилищ уже применёнными.

> **Ограничение.** Две операции с одной и той же таблицей в одном тесте --- сначала с действием,
> потом без --- выразить нельзя: состояния init'ов одного типа сливаются. Для такого редкого случая
> используйте два `<test type="Part">`.

*** ** * ** ***

### 5. BeanInit --- Вызов методов бинов

**Назначение:** Выполнение методов на Spring-бинах перед тестом (без параметров).
**Важно:** `BeanInit` --- это **действие** , а не состояние. Методы выполняются немедленно при применении инициализации,
результат не сохраняется между тестами.

#### Примеры

```
xml
<!-- Очистка кэша перед тестом -->
<init type="beanInit">
    <bean name="cacheService" method="clear"/>
    <bean name="dataLoader" method="loadTestData"/>
</init>

```

```
xml
<!-- Инициализация только для TestCase -->
<init type="beanInit" applyTo="TestCase">
    <bean name="setupService" method="init"/>
</init>

```

*** ** * ** ***

### 6. BeanPropertyInit --- Декларативная настройка свойств бинов

**Назначение:** Установка полей и JavaBean-свойств Spring-бинов на время теста с **автоматическим откатом**.
**Важно:** в отличие от `BeanInit`, это **состояние**: перед первой модификацией запоминается исходное значение, и при
выходе объявления за область видимости (кейс или контейнер) значение восстанавливается.

#### Примеры

```
xml
<!-- Один бин: имя указывается на уровне <init> -->
<init type="BeanPropertyInit" bean="candleJobFactory">
    <property name="startUpHistoryAmount" type="PeriodDuration">PT0M</property>
    <property name="dynamicCandlesFlag" type="Boolean">true</property>
</init>

```

```
xml
<!-- Несколько бинов: имя задаётся у каждого <property> -->
<init type="BeanPropertyInit">
    <property bean="candleIteratorFactory" name="dynamicCandlesFlag" type="Boolean">true</property>
    <property bean="candleJobFactory" name="startUpHistoryAmount" type="PeriodDuration">PT0M</property>
</init>

```

```
xml
<!-- Null-значение: пустой элемент (не xsi:nil --- он обнулит сам <property>) -->
<init type="BeanPropertyInit" bean="myService">
    <property name="optionalRule"/>
</init>

```

```
xml
<!-- Коллекция: элементы вкладываются в тело свойства -->
<init type="BeanPropertyInit" bean="myService">
    <property name="allowedCurrencies" type="ArrayList">
        <value>RUB</value>
        <value>USD</value>
    </property>
</init>

```

```
json
{
  "type": "BeanPropertyInit",
  "bean": "candleJobFactory",
  "property": [
    {"name": "startUpHistoryAmount", "value": ["PeriodDuration", "PT0M"]},
    {"name": "dynamicCandlesFlag", "value": true}
  ]
}
```

#### Параметры

| Атрибут   | Описание                                                     |
|-----------|--------------------------------------------------------------|
| `bean`    | Имя Spring-бина по умолчанию для всех вложенных `<property>` |
| `name`    | Псевдоним `bean` (не переопределяет явно заданный `bean`)    |
| `applyTo` | Область применения: `TestCase`, `TestPart`                   |

| Атрибут `<property>` | Описание                                                        |
|----------------------|-----------------------------------------------------------------|
| `bean`               | Имя бина (переопределяет `bean` у `<init>`)                     |
| `name`               | Имя поля или JavaBean-свойства (getter/setter предпочтительнее) |
| `type`               | Тип значения (как у обычных элементов теста)                    |

**Правила:**

* Значение ищется через getter/`is`-getter, затем через приватное поле; запись --- через setter, затем через поле.
  Значение приводится к типу сеттера (для примитивов несовместимый тип --- ошибка).
* `null` записывается как есть. Примитивное поле или сеттер не могут его принять --- инициализация падает с ошибкой.
* Исходное значение запоминается один раз --- перед первой модификацией за всё время жизни контекста.
* Переопределение на вложенном уровне действует до выхода из него: после кейса значение возвращается к значению
  контейнера, после контейнера --- к исходному значению Spring-контекста.
* Как и у `DateTimeInit`, изменения на уровне `TestPart` **накапливаются** внутри кейса: откат происходит при выходе из
  кейса, а не между частями.
* Свойство должно быть читаемым (getter или поле), иначе исходное значение нечем запомнить --- будет ошибка.
* Настройка применяется к singleton-бинам; прототипы и статические поля не поддерживаются.
* Атрибут `type` в XML --- это имя типа развёрнутого значения (по умолчанию для unwrapped-значений используется `utype`;
  здесь он переопределён аннотацией `@UnwrappedTypeProperty("type")`).

*** ** * ** ***

### 7. StubInit --- Сброс стабов

**Назначение:** Сброс состояния всех стабов в иерархии тестов.
**Что именно сбрасывается:**

* Счётчик результатов `<result>` в `<stubCall>` возвращается к первому значению
* При нескольких `<result>` следующий вызов стаба начнёт с первого результата, а не продолжит последовательность
* Все стабы в текущем тесте и родителях (при `resetAll="true"`)

#### Примеры

```
xml
<!-- Сбросить все стабы -->
<init type="StubInit" resetAll="true"/>

```

```
xml
<!-- Сброс только для TestCase -->
<init type="StubInit" applyTo="TestCase" resetAll="true"/>

```

```
xml
<!-- Сброс только для TestPart -->
<init type="StubInit" applyTo="TestPart" resetAll="true"/>

```

#### Поведение

| `resetAll`          | Поведение                                                    |
|---------------------|--------------------------------------------------------------|
| `true`              | Сбрасываются все `StubCall` в текущем тесте и всех родителях |
| `false` / не указан | Никаких действий                                             |

#### Пример с последовательностью результатов

```
xml
<test type="Container">
    <!-- Стаб с тремя результатами -->
    <stubCall name="idGenerator" method="generate">
        <result><return>ID-001</return></result>
        <result><return>ID-002</return></result>
        <result><return>ID-003</return></result>
    </stubCall>

    <test type="Case" name="тест1">
        <init type="StubInit" resetAll="true"/>
        <!-- Первый вызов вернёт ID-001 (счётчик сброшен) -->
    </test>

    <test type="Case" name="тест2">
        <init type="StubInit" resetAll="true"/>
        <!-- Первый вызов снова вернёт ID-001 (счётчик сброшен) -->
    </test>
</test>

```

*** ** * ** ***

### 8. KeyValueStorageInit --- Инициализация key-value хранилища

**Назначение:** Очистка и загрузка данных в key-value хранилища (Redis, in-memory cache).

#### Примеры

```
xml
<!-- Очистить и загрузить данные -->
<init type="keyValueStorageInit" name="redisCache" clear="true">
    <map type="LinkedHashMapStringObject">
        <entry key="user_123">John Doe</entry>
        <entry key="config_timeout" type="Integer">5000</entry>
        <entry key="feature_enabled" type="Boolean">true</entry>
    </map>
</init>

```

```
xml
<!-- Только загрузить данные без очистки -->
<init type="keyValueStorageInit" name="localCache">
    <map type="LinkedHashMapStringObject">
        <entry key="feature_enabled" type="Boolean">true</entry>
    </map>
</init>

```

```
xml
<!-- Только очистить хранилище -->
<init type="keyValueStorageInit" name="redisCache" clear="true"/>

```

#### Параметры

| Атрибут | Описание                                                |
|---------|---------------------------------------------------------|
| `name`  | Имя хранилища (совпадает с бином `KeyValueDataStorage`) |
| `clear` | Очистить все данные перед загрузкой                     |
| `map`   | Ключ-значение пары для загрузки                         |

#### Формат ключей в map

⚠️ **Важно:** Ключи с двоеточием (например, `user:123`) **нельзя** использовать напрямую в XML как имя тега --- это
зарезервировано для пространств имён. Используйте `LinkedHashMapStringObject` с атрибутом `key`:

```
xml
<!-- ✅ Правильно: через LinkedHashMapStringObject с атрибутом key -->
<init type="keyValueStorageInit" name="redisCache" clear="true">
    <map type="LinkedHashMapStringObject">
        <entry key="user:123">John Doe</entry>
        <entry key="user:456">Jane Doe</entry>
        <entry key="config:timeout" type="Integer">5000</entry>
        <entry key="session:abc123">active</entry>
    </map>
</init>

<!-- ❌ Ошибка: двоеточие в имени тега -->
<init type="keyValueStorageInit" name="redisCache" clear="true">
    <map>
        <user:123>John Doe</user:123>
    </map>
</init>

```

**Почему используется `LinkedHashMapStringObject`:**

| Проблема                                                      | Решение                                                                |
|---------------------------------------------------------------|------------------------------------------------------------------------|
| Ключи Redis содержат двоеточие (`user:123`, `config:timeout`) | Атрибут `key="user:123"` позволяет использовать любые символы          |
| Порядок ключей важен для тестов                               | `LinkedHashMap` сохраняет порядок вставки                              |
| Нужны разные типы значений                                    | Атрибут `type` указывает тип значения (`Integer`, `Boolean`, `String`) |

**Типы значений:**

* Типы указываются с заглавной буквы: `String`, `Integer`, `Boolean`
* `type="String"` можно не указывать --- строка является типом по умолчанию

*** ** * ** ***

Порядок выполнения инициализаций
--------------------------------

| Порядок   | Тип инициализации     | Описание                                      |
|-----------|-----------------------|-----------------------------------------------|
| **0**     | `DateTimeInit`        | Время устанавливается первым                  |
| **500**   | `BeanPropertyInit`    | Свойства бинов (до БД и вызовов методов)      |
| **1000**  | `SqlStorageSetup`     | Настройка схемы и данных БД                   |
| **2000**  | `SqlStorageInit`      | Разрешение таблиц, загрузка данных            |
| **3000**  | `KeyValueStorageInit` | Инициализация кэшей                           |
| **10000** | `BeanInit`            | Вызов методов бинов (после подготовки данных) |

**Важно:** Меньшие значения `order` выполняются **раньше** .

*** ** * ** ***

Жизненный цикл инициализации
----------------------------

```
┌─────────────────────────────────────────────────────────────┐
│              TestExecutor.before(test)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│         InitializationService.beforeTest(test)              │
│  ├── Для каждого InitSetup (по порядку):                    │
│  │   ├── add(test) --- добавить тест в builder               │
│  │   ├── add(init) --- добавить инициализацию из теста       │
│  │   └── build(test) --- построить и применить состояние     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              TestExecutor.runTest()                         │
│  (Выполнение теста: bean/method или inboundMessage)         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              TestExecutor.after(test)                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│         InitializationService.afterTest(test)               │
│  ├── Удалить init из теста                                  │
│  └── Для каждого InitSetup:                                 │
│      └── remove(test) --- удалить состояние теста            │
└─────────────────────────────────────────────────────────────┘

```

*** ** * ** ***

Наследование и область применения
---------------------------------

### Модель наследования

Настройки инициализации наследуются **сверху вниз** по иерархии тестов:

```
xml
<test type="Container" name="модуль">
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>
    <init type="beanInit">
        <bean name="setupService" method="init"/>
    </init>

    <test type="Case" name="кейс1">
        <!-- Наследует dateTimeInit и beanInit от родителя -->
        <init type="sqlStorageInit" name="dataSource">
            <tablesToClear>t1</tablesToClear>
        </init>
    </test>

    <test type="Case" name="кейс2">
        <!-- Наследует dateTimeInit и beanInit от родителя -->
        <!-- Не наследует sqlStorageInit из кейс1 -->
    </test>
</test>

```

### ⚠️ Важное правило для TestPart

**`TestPart` --- это часть одного сценария.** К ним **НЕ применяются** init, которые наследуются от родителей, **кроме
** :

1. Инициализаций с явно указанным `applyTo="TestPart"`
2. Инициализаций, объявленных непосредственно в самом `TestPart`

```
xml
<test type="Container">
    <!-- Этот init НЕ применится к TestPart (нет applyTo="TestPart") -->
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>

    <test type="Case" name="многошаговый">
        <!-- Этот init применится ко всем Part внутри кейса -->
        <init type="dateTimeInit" applyTo="TestPart" addDuration="PT1M"/>

        <test type="Part" name="шаг1">
            <!-- Время: 2025-01-01T00:01:00Z (только от applyTo="TestPart") -->
        </test>

        <test type="Part" name="шаг2">
            <!-- Время: 2025-01-01T00:02:00Z (накопленный сдвиг) -->
            <!-- Можно переопределить локально -->
            <init type="dateTimeInit" dateTime="2025-02-02T00:00:00Z"/>
        </test>
    </test>
</test>

```

### Область применения `applyTo`

| `applyTo`       | Применяется к       | Наследуется |
|-----------------|---------------------|-------------|
| Не указан       | Все типы тестов     | Да          |
| `TestContainer` | Только контейнеры   | Да          |
| `TestCase`      | Только тест-кейсы   | Да          |
| `TestPart`      | Только части тестов | Да          |

**Пример массового применения:**

```
xml
<!-- Применится ко всем TestCase внутри этого контейнера одной записью -->
<test type="Container">
    <init type="StubInit" applyTo="TestCase" resetAll="true"/>

    <test type="Case" name="кейс1">
        <!-- StubInit применён -->
    </test>

    <test type="Case" name="кейс2">
        <!-- StubInit применён -->
        <test type="Part" name="шаг1">
            <!-- StubInit НЕ применён (applyTo="TestCase") -->
        </test>
    </test>
</test>

```

*** ** * ** ***

Управление состоянием
---------------------

### Слияние состояний

Когда несколько инициализаций применяются к одному тесту, их состояния **сливаются** :

```
java
// Псевдокод слияния
currentState = currentState.merge(newState);

```

**Правила слияния:**

| Тип инициализации     | Правила слияния                                   |
|-----------------------|---------------------------------------------------|
| `DateTimeInit`        | `dateTime` задаёт базу и сбрасывает `addDuration` |
| `BeanInit`            | Все бины добавляются в набор                      |
| `StubInit`            | `resetAll=true` переопределяет всё                |
| `SqlStorageInit`      | Таблицы объединяются, состояние сравнивается      |
| `KeyValueStorageInit` | `clear=true` очищает, `map` объединяется          |

`addDuration` суммируется с текущей базой `dateTime`.

### Сравнение состояний

Перед применением инициализации фреймворк **сравнивает** старое и новое состояние:

```
java
if (oldState.equals(newState)) {
    // Никаких действий не требуется
    return;
}
// Применяются только необходимые изменения
applyChanges(oldState, newState);

```

Это позволяет **оптимизировать** выполнение тестов, избегая лишних операций.

*** ** * ** ***

Примеры комплексной конфигурации
--------------------------------

### Пример 1: Полная настройка тестового окружения

```
xml
<?xml version='1.1' encoding='UTF-8'?>
<test type="Container">
    <!-- Глобальные настройки для всех тестов -->
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>

    <init type="sqlStorageSetup" name="dataSource">
        <sqlFilePath>schema.sql</sqlFilePath>
        <dbUnitPath>initializationData.xml</dbUnitPath>
    </init>
    <init type="OnTableChangeInit" name="dataSource" applyTo="TestCase">
        <onTableChange tableName="users" beanName="userService" beanMethod="reloadUsers"/>
    </init>

    <test type="Case" name="созданиеПользователя">
        <!-- Настройки только для этого кейса -->
        <init type="sqlStorageInit" name="dataSource">
            <tablesToClear>users</tablesToClear>
            <tablesToLoad>users</tablesToLoad>
        </init>

        <bean>userService</bean>
        <method>createUser</method>
        <request>
            <value type="String">John</value>
        </request>
        <response type="User">
            <id>1</id>
            <name>John</name>
        </response>
    </test>

    <test type="Case" name="удалениеПользователя">
        <!-- Наследует dateTimeInit и sqlStorageSetup от родителя -->
        <init type="sqlStorageInit" name="dataSource">
            <tablesToClear>users</tablesToClear>
        </init>

        <bean>userService</bean>
        <method>deleteUser</method>
        <request>
            <value type="Integer">1</value>
        </request>
        <response>ok</response>
    </test>
</test>

```

### Пример 2: Многошаговый тест с инициализацией

```
xml
<?xml version='1.1' encoding='UTF-8'?>
<test type="Container">
    <test type="Case" name="полныйЦиклЗаказа">
        <!-- Инициализация применяется один раз перед всеми Part -->
        <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>
        <init type="sqlStorageInit" name="dataSource">
            <tablesToClear>orders,payments</tablesToClear>
            <tablesToLoad>orders</tablesToLoad>
        </init>

        <!-- Шаг 1: Создание заказа -->
        <test type="Part" name="создание">
            <bean>orderService</bean>
            <method>create</method>
            <response type="Order">
                <id>1</id>
                <status>CREATED</status>
            </response>
        </test>

        <!-- Шаг 2: Оплата (видит результат шага 1) -->
        <test type="Part" name="оплата">
            <init type="dateTimeInit" addDuration="PT1M"/>
            <bean>paymentService</bean>
            <method>pay</method>
            <request>
                <value type="Integer">1</value>
            </request>
            <response>ok</response>
        </test>

        <!-- Шаг 3: Доставка (видит результаты шагов 1-2) -->
        <test type="Part" name="доставка">
            <init type="dateTimeInit" addDuration="PT30M"/>
            <bean>deliveryService</bean>
            <method>ship</method>
            <request>
                <value type="Integer">1</value>
            </request>
            <response>ok</response>
        </test>
    </test>
</test>

```

### Пример 3: Сброс стабов между тестами

```
xml
<?xml version='1.1' encoding='UTF-8'?>
<test type="Container">
    <!-- Общие стабы для всех тестов -->
    <stubCall name="idGenerator" method="generate">
        <result><return type="String">ID-001</return></result>
        <result><return type="String">ID-002</return></result>
    </stubCall>

    <test type="Case" name="тест1">
        <!-- Сбросить стабы перед этим тестом -->
        <init type="StubInit" resetAll="true"/>

        <bean>orderService</bean>
        <method>create</method>
        <response type="Order">
            <id>ID-001</id>
        </response>
    </test>

    <test type="Case" name="тест2">
        <!-- Сбросить стабы перед этим тестом -->
        <init type="StubInit" resetAll="true"/>

        <bean>orderService</bean>
        <method>create</method>
        <response type="Order">
            <id>ID-001</id>
        </response>
    </test>
</test>

```

*** ** * ** ***

Частые проблемы
---------------

### Инициализация не применяется

**Причина:** Несоответствие имени хранилища или бина.
**Решение:**

```
xml
<!-- ✅ Правильно: имя совпадает с бином в Spring-контексте -->
<init type="sqlStorageInit" name="dataSource"/>

<!-- ❌ Ошибка: имя не совпадает -->
<init type="sqlStorageInit" name="wrongName"/>

```

### Конфликт состояний инициализации

**Причина:** Несколько инициализаций с противоречивыми настройками.
**Решение:**

```
xml
<!-- ✅ Правильно: явное переопределение в дочернем тесте -->
<test type="Container">
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>
    <test type="Case" name="кейс">
        <init type="dateTimeInit" dateTime="2025-02-02T00:00:00Z"/>
    </test>
</test>

```

### Действия при изменении таблиц не вызываются

**Причина:** Неправильное имя таблицы, отсутствующий `OnTableChangeInit` в области видимости или
перекрывающий `OnTableChangeInit` с пустым списком.
**Решение:** Убедитесь, что имя таблицы совпадает с БД и что `OnTableChangeInit` виден в текущем тесте.
Если действия намеренно отключены пустым `OnTableChangeInit`, они будут вызваны на следующем тесте,
где инициализация снова активна.

```
xml
<!-- ✅ Правильно: имя таблицы совпадает с БД -->
<onTableChange tableName="users" beanName="userService" beanMethod="reloadUsers"/>

<!-- ❌ Ошибка: имя таблицы не совпадает -->
<onTableChange tableName="User" beanName="userService" beanMethod="reloadUsers"/>

```

### Инициализация не применяется к TestPart

**Причина:** Инициализация объявлена в родителе без `applyTo="TestPart"`.
**Решение:**

```
xml
<!-- ✅ Правильно: явно указать applyTo="TestPart" -->
<test type="Case" name="многошаговый">
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>
    <init type="dateTimeInit" applyTo="TestPart" addDuration="PT1M"/>

    <test type="Part" name="шаг1">
        <!-- Инициализация применится -->
    </test>
</test>

<!-- ❌ Ошибка: без applyTo="TestPart" не применится к Part -->
<test type="Case" name="многошаговый">
    <init type="dateTimeInit" dateTime="2025-01-01T00:00:00Z"/>
    <init type="dateTimeInit" addDuration="PT1M"/>

    <test type="Part" name="шаг1">
        <!-- Инициализация НЕ применится -->
    </test>
</test>

```

*** ** * ** ***

Рекомендации
------------

1. **Выносите общие настройки на уровень контейнера** --- `dateTimeInit`, `sqlStorageSetup` можно определить в
   родительском `Container` для переиспользования.
2. **Используйте `applyTo` для массового применения** --- например, `StubInit` с `applyTo="TestCase"` применится ко всем
   кейсам в контейнере одной записью.
3. **Для `TestPart` всегда указывайте `applyTo="TestPart"`** --- если инициализация должна применяться к шагам
   многошагового теста.
4. **Минимизируйте количество инициализаций** --- каждая инициализация добавляет накладные расходы, используйте только
   необходимое.
5. **Проверяйте имена хранилищ и бинов** --- убедитесь, что `name` в инициализации совпадает с именами бинов в
   Spring-контексте.
6. **Помните о порядке выполнения** --- если инициализация зависит от другой, убедитесь, что `order` настроен правильно.
7. **Изолируйте тесты с помощью `StubInit`** --- используйте `resetAll="true"` для сброса стабов между тестами.
8. **Используйте `loadAllTables` только при необходимости** --- загрузка всех таблиц может замедлить тесты.
9. **Для сложных ключей в KeyValueStorageInit используйте `LinkedHashMapStringObject`** --- ключи с
   двоеточием (`user:123`, `config:timeout`) требуют специального формата через `entry key="..."`.
10. **Помните: `TestPart` не наследует init от родителей** --- кроме тех, у которых явно указано `applyTo="TestPart"`
    или которые объявлены непосредственно в `Part`.
