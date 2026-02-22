Документация подсистемы инициализации тестов
============================================

Обзор
-----

Подсистема инициализации тестов предоставляет механизм для настройки состояния окружения перед выполнением тестов. Она
позволяет декларативно конфигурировать различные аспекты тестовой среды через XML-файлы, включая время, данные в БД,
состояние бинов, моки и хранилища ключ-значение.

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
            <tablesToChange>users</tablesToChange>  
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
    <init type="mockInit" applyTo="TestCase" resetAll="true"/>  
    
    <test type="Case" name="кейс1">  
        <!-- mockInit применён автоматически -->  
    </test>  
    <test type="Case" name="кейс2">  
        <!-- mockInit применён автоматически -->  
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

*** ** * ** ***

### 2. SqlStorageSetup --- Настройка SQL-хранилища

**Назначение:** Первоначальная конфигурация БД: выполнение SQL-скриптов, загрузка DBUnit-датасетов, настройка хуков.

#### Примеры

```
xml  
<!-- Базовая настройка с DBUnit и хуком -->  
<init type="sqlStorageSetup" name="dataSource">  
    <dbUnitPath>initializationData.xml</dbUnitPath>  
    <tableHook tableName="t1" beanName="testSQLDataStorage" beanMethod="reloadT1"/>  
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

| Элемент       | Описание                                                      |
|---------------|---------------------------------------------------------------|
| `name`        | Имя хранилища (совпадает с бином `SQLDataStorageService`)     |
| `sqlFilePath` | Пути к SQL-файлам (classpath)                                 |
| `sql`         | Inline SQL-запросы                                            |
| `dbUnitPath`  | Пути к DBUnit XML-датасетам                                   |
| `tableHook`   | Хуки для автоматического обновления кэша при изменении таблиц |

*** ** * ** ***

### 3. SqlStorageInit --- Инициализация таблиц

**Назначение:** Управление доступом к таблицам и загрузка данных **перед конкретным тестом** .

#### Примеры

```
xml  
<!-- Разрешить изменение конкретных таблиц -->  
<init type="sqlStorageInit" name="dataSource">  
    <tablesToChange>t1,t2</tablesToChange>  
</init>  

```

```
xml  
<!-- Загрузить все таблицы из DBUnit -->  
<init type="sqlStorageInit" name="dataSource" loadAllTables="true"/>  

```

```
xml  
<!-- Разрешить и загрузить конкретные таблицы -->  
<init type="sqlStorageInit" name="dataSource">  
    <tablesToChange>t1</tablesToChange>  
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

| Атрибут          | Описание                                                        |
|------------------|-----------------------------------------------------------------|
| `name`           | Имя хранилища                                                   |
| `tablesToChange` | Список таблиц, которые можно изменять (остальные заблокированы) |
| `tablesToLoad`   | Список таблиц для загрузки данных из DBUnit                     |
| `loadAllTables`  | `true` --- загрузить все таблицы из датасета                    |
| `sql`            | SQL-запросы для выполнения                                      |

*** ** * ** ***

### 4. BeanInit --- Вызов методов бинов

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

### 5. MockInit --- Сброс моков

**Назначение:** Сброс состояния всех моков в иерархии тестов.  
**Что именно сбрасывается:**

* Счётчик результатов `<result>` в `<mockInvoke>` возвращается к первому значению
* При нескольких `<result>` следующий вызов мока начнёт с первого результата, а не продолжит последовательность
* Все моки в текущем тесте и родителях (при `resetAll="true"`)

#### Примеры

```
xml  
<!-- Сбросить все моки -->  
<init type="mockInit" resetAll="true"/>  

```

```
xml  
<!-- Сброс только для TestCase -->  
<init type="mockInit" applyTo="TestCase" resetAll="true"/>  

```

```
xml  
<!-- Сброс только для TestPart -->  
<init type="mockInit" applyTo="TestPart" resetAll="true"/>  

```

#### Поведение

| `resetAll`          | Поведение                                                      |
|---------------------|----------------------------------------------------------------|
| `true`              | Сбрасываются все `MockInvoke` в текущем тесте и всех родителях |
| `false` / не указан | Никаких действий                                               |

#### Пример с последовательностью результатов

```
xml  
<test type="Container">  
    <!-- Mock с тремя результатами -->  
    <mockInvoke name="idGenerator" method="generate">  
        <result><return>ID-001</return></result>  
        <result><return>ID-002</return></result>  
        <result><return>ID-003</return></result>  
    </mockInvoke>  
    
    <test type="Case" name="тест1">  
        <init type="mockInit" resetAll="true"/>  
        <!-- Первый вызов вернёт ID-001 (счётчик сброшен) -->  
    </test>  
    
    <test type="Case" name="тест2">  
        <init type="mockInit" resetAll="true"/>  
        <!-- Первый вызов снова вернёт ID-001 (счётчик сброшен) -->  
    </test>  
</test>  

```

*** ** * ** ***

### 6. KeyValueStorageInit --- Инициализация key-value хранилища

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
            <tablesToChange>t1</tablesToChange>  
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
    <init type="mockInit" applyTo="TestCase" resetAll="true"/>  
    
    <test type="Case" name="кейс1">  
        <!-- mockInit применён -->  
    </test>  
    
    <test type="Case" name="кейс2">  
        <!-- mockInit применён -->  
        <test type="Part" name="шаг1">  
            <!-- mockInit НЕ применён (applyTo="TestCase") -->  
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

| Тип инициализации     | Правила слияния                                      |
|-----------------------|------------------------------------------------------|
| `DateTimeInit`        | `dateTime` переопределяет, `addDuration` суммируется |
| `BeanInit`            | Все бины добавляются в набор                         |
| `MockInit`            | `resetAll=true` переопределяет всё                   |
| `SqlStorageInit`      | Таблицы объединяются, состояние сравнивается         |
| `KeyValueStorageInit` | `clear=true` очищает, `map` объединяется             |

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
        <tableHook tableName="users" beanName="userService" beanMethod="reloadUsers"/>  
    </init>  
    
    <test type="Case" name="созданиеПользователя">  
        <!-- Настройки только для этого кейса -->  
        <init type="sqlStorageInit" name="dataSource">  
            <tablesToChange>users</tablesToChange>  
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
            <tablesToChange>users</tablesToChange>  
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
            <tablesToChange>orders,payments</tablesToChange>  
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

### Пример 3: Сброс моков между тестами

```
xml  
<?xml version='1.1' encoding='UTF-8'?>  
<test type="Container">  
    <!-- Общие моки для всех тестов -->  
    <mockInvoke name="idGenerator" method="generate">  
        <result><return type="String">ID-001</return></result>  
        <result><return type="String">ID-002</return></result>  
    </mockInvoke>  
    
    <test type="Case" name="тест1">  
        <!-- Сбросить моки перед этим тестом -->  
        <init type="mockInit" resetAll="true"/>  
        
        <bean>orderService</bean>  
        <method>create</method>  
        <response type="Order">  
            <id>ID-001</id>  
        </response>  
    </test>  
    
    <test type="Case" name="тест2">  
        <!-- Сбросить моки перед этим тестом -->  
        <init type="mockInit" resetAll="true"/>  
        
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

### Хуки таблиц не вызываются

**Причина:** Неправильное имя таблицы.  
**Решение:**

```
xml  
<!-- ✅ Правильно: имя таблицы совпадает с БД -->  
<tableHook tableName="users" beanName="userService" beanMethod="reloadUsers"/>  

<!-- ❌ Ошибка: имя таблицы не совпадает -->  
<tableHook tableName="User" beanName="userService" beanMethod="reloadUsers"/>  

```

### Инициализация не применяется к TestPart

**Причина:** Инициализация объявлена в родителе без `applyTo="TestPart"`.  
**Решение:**

```
xml  
<!-- ✅ Правильно: явно указать applyTo="TestPart" -->  
<test type="Case" name="многошаговый">  
    <init type="dateTimeInit" applyTo="TestPart" addDuration="PT1M"/>  
    
    <test type="Part" name="шаг1">  
        <!-- Инициализация применится -->  
    </test>  
</test>  

<!-- ❌ Ошибка: без applyTo="TestPart" не применится к Part -->  
<test type="Case" name="многошаговый">  
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
2. **Используйте `applyTo` для массового применения** --- например, `MockInit` с `applyTo="TestCase"` применится ко всем
   кейсам в контейнере одной записью.
3. **Для `TestPart` всегда указывайте `applyTo="TestPart"`** --- если инициализация должна применяться к шагам
   многошагового теста.
4. **Минимизируйте количество инициализаций** --- каждая инициализация добавляет накладные расходы, используйте только
   необходимое.
5. **Проверяйте имена хранилищ и бинов** --- убедитесь, что `name` в инициализации совпадает с именами бинов в
   Spring-контексте.
6. **Помните о порядке выполнения** --- если инициализация зависит от другой, убедитесь, что `order` настроен правильно.
7. **Изолируйте тесты с помощью `MockInit`** --- используйте `resetAll="true"` для сброса моков между тестами.
8. **Используйте `loadAllTables` только при необходимости** --- загрузка всех таблиц может замедлить тесты.
9. **Для сложных ключей в KeyValueStorageInit используйте `LinkedHashMapStringObject`** --- ключи с
   двоеточием (`user:123`, `config:timeout`) требуют специального формата через `entry key="..."`.
10. **Помните: `TestPart` не наследует init от родителей** --- кроме тех, у которых явно указано `applyTo="TestPart"`
    или которые объявлены непосредственно в `Part`.  
