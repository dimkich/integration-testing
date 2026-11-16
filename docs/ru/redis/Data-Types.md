Подготовка данных и Снимки базы
================================

[English version](../../en/redis/Data-Types.md)

Тестирование логики с Redis всегда состоит из двух шагов:

1. **Запись начального состояния (Seed):** С помощью блока `<init>` вы наполняете Redis тестовыми ключами.
2. **Проверка изменений (Assert):** С помощью блока `<diff>` вы описываете, какие ключи должны
   были добавиться, измениться или удалиться в базе после выполнения кода вашего приложения.

Решение проблемы двоеточий в ключах Redis
-----------------------------------------

В Redis принято разделять логические части ключа двоеточием, например: `user:profile:100`.
Однако в формате XML символ двоеточия зарезервирован для пространств имён (`namespace:tag`). Если вы
попытаетесь написать тег `<user:profile:100>`, XML-парсер выдаст ошибку синтаксиса.

Поэтому для ключей используйте тип `LinkedHashMapStringObject`: имя техническое, но оно даёт главное —
ключ передаётся обычной строкой в атрибуте `key="..."` внутри тега `<entry>`, где допустимы любые
символы, включая двоеточия:

```xml
<!-- Пример правильной записи ключа с двоеточием -->
<map type="LinkedHashMapStringObject">
    <entry key="user:profile:100">ACTIVE</entry>
</map>
```

В примерах `<init>` ниже карта ключей записывается именно так — просто подставляйте свои ключи в
атрибут `key`.

Инициализация базы через `<init>`
---------------------------------

Секция инициализации описывается в начале контейнера или тест-кейса. Для Redis используется тип
инициализатора `KeyValueStorageInit`:

```xml

<init type="KeyValueStorageInit" name="имя_подключения" clear="true/false">
    <map type="LinkedHashMapStringObject">
        <!-- Ваши данные -->
    </map>
</init>
```

* `name` — имя подключения из `integration.testing.redis.connections` (например,
  `redissonConnectionFactory`).
* `clear="true"` — очистить базу перед тестом. Если in-memory mirror пуст, команда `FLUSHALL` не
  отправляется.
* `clear="false"` — не очищать базу (новые ключи допишутся или обновят существующие).

Инициализатор поддерживает два формата записи данных: **Сокращённый** и **Полный**.

### Сокращённый формат (Abbreviated Format)

Используется для быстрой записи простых строк, чисел или флагов, когда у ключей нет времени жизни (TTL).

В этом формате значение ключа записывается текстом прямо внутри тега `<entry>`:

```xml

<init type="KeyValueStorageInit" name="redissonConnectionFactory" clear="true">
    <map type="LinkedHashMapStringObject">
        <!-- Будет сохранено как строка (String) -->
        <entry key="global:app:status">ACTIVE</entry>

        <!-- Будет сохранено как строка "5000" (тип Integer в тесте подскажет парсеру тип значения) -->
        <entry key="config:timeout" type="Integer">5000</entry>

        <!-- Будет сохранено как строка "true" -->
        <entry key="feature:flag" type="Boolean">true</entry>

        <!-- Бинарные данные в кодировке Base64 -->
        <entry key="security:salt" type="byte[]">gQ==</entry>
    </map>
</init>
```

### Полный формат (Full Format)

Используется, когда вам необходимо задать время жизни ключа (`ttl`) или записать сложные структуры
данных (хэши, списки, множества, упорядоченные множества, стримы).

При полной записи значение ключа помечается `utype="RedisEntry"`, а внутри располагаются теги
`<data>` (само значение) и `<ttl>` (длительность жизни):

```xml

<init type="KeyValueStorageInit" name="redissonConnectionFactory" clear="true">
    <map type="LinkedHashMapStringObject">

        <!-- Пример строки с временем жизни 5 минут -->
        <entry key="temp:otp:code" utype="RedisEntry">
            <data>9941</data>
            <ttl>PT5M</ttl> <!-- Формат ISO-8601: PT5M = 5 минут, PT30S = 30 секунд -->
        </entry>

    </map>
</init>
```

> **Совет:** Если TTL не требуется, используйте сокращённую запись без `RedisEntry` — примеры для
> каждого типа данных приведены ниже в соответствующих разделах.

Проверка изменений через `<diff>`
--------------------------------------------

Блок `<diff>` описывает, какие изменения произошли в Redis после работы вашего приложения.
Он строится на основе сравнения состояния «до» и «после» выполнения теста.

Запись хранилища — это ключ Redis (`key`), а при `multipleDatabases` — `db/key`. Операция задаётся
тегом элемента (`insert`, `update` или `delete`). Тело — значение ключа: строка пишется текстом, структура —
полями или элементами, TTL — атрибутом `ttl`. Значение, которого не было в базе, пишется как
`insert`, изменившееся — `update`, исчезнувшее — `delete`.

Пример: ключ удалён:

```xml
<diff storage="redissonConnectionFactory">
    <delete key="user:session:100"/>
</diff>
```

Пример: строка с TTL:

```xml
<diff storage="redissonConnectionFactory">
    <insert key="user:session:100" ttl="PT30M">ACTIVE_SESSION</insert>
</diff>
```

Пример: у хэша добавилось поле со своим TTL:

```xml
<diff storage="redissonConnectionFactory">
    <update key="user:profile:100" type="RedisHash">
        <insert key="temp_token" ttl="PT30S">token_abc_123</insert>
    </update>
</diff>
```

TTL пишется атрибутом у записи. Как и остальные атрибуты, он описывает состояние после теста:
`update` повторяет `ttl`, даже если TTL не изменился, а отсутствие атрибута значит, что TTL нет.
Уменьшение TTL от сдвига времени само по себе изменением не считается: снимки «до» и «после»
пересчитываются на один момент.

Поле хэша тоже может быть объектом: если тип field-ключа структурный, имя поля пишется
структурой `<key>` — так же, как ключ любой другой мапы; чтобы свернуть его в строку, верните
скаляр из `addDiffKeyPresenter` (см.
[Расширение diff](../Storage-Diff-Extensibility.md#ключ-записи--adddiffkeypresenter)).

XML-представление для всех структур данных Redis (Полный формат)
----------------------------------------------------------------

Ниже приведены готовые шаблоны для записи любых типов данных Redis в полном формате. Используйте
их в `<init>` (для подготовки состояния) — это формат значения, а не diff. `<diff>`
строится по правилам выше; общий контракт собран в разделе
[Изменения хранилищ](../Storage-Diff.md).

### HASH (Хэши / Таблицы)

В Redis хэш хранит набор полей и значений. В нашей системе он представляется типом `RedisHash`.
Каждое поле хэша является самостоятельным элементом `RedisEntry`, поэтому каждому полю хэша можно
задать свой собственный TTL (команда `HPEXPIREAT`, Redis 7.4+).

```xml
<entry key="user:profile:100" utype="RedisEntry">
    <data type="RedisHash">
        <!-- Обычное поле без TTL -->
        <entry>
            <key>username</key>
            <value type="RedisEntry">
                <data>Ivan</data>
            </value>
        </entry>
        <!-- Поле с индивидуальным временем жизни в 30 секунд -->
        <entry>
            <key>temp_token</key>
            <value type="RedisEntry">
                <data>token_abc_123</data>
                <ttl>PT30S</ttl>
            </value>
        </entry>
    </data>
</entry>
```

**Сокращённый формат (без TTL):**

```xml
<entry key="user:profile:100" utype="RedisHash">
    <entry>
        <key>username</key>
        <value>
            <data>Ivan</data>
        </value>
    </entry>
    <entry>
        <key>role</key>
        <value>
            <data>admin</data>
        </value>
    </entry>
</entry>
```

### LIST (Списки)

Списки сохраняют строгий порядок добавления элементов. Представляются типом `RedisList`.

```xml
<entry key="notifications:queue" utype="RedisEntry">
    <data type="RedisList">
        <value>первое_сообщение</value>
        <value>второе_сообщение</value>
    </data>
</entry>
```

**Сокращённый формат (без TTL):**

```xml
<entry key="notifications:queue" utype="RedisList">
    <value>первое_сообщение</value>
    <value>второе_сообщение</value>
</entry>
```

### SET (Множества)

Множества содержат только уникальные элементы. Элементы сортируются `RedisDataComparator`:
строки — лексикографически, числа — численно. Представляются типом `RedisSet`.

```xml
<entry key="user:roles:1" utype="RedisEntry">
    <data type="RedisSet">
        <value>ROLE_ADMIN</value>
        <value>ROLE_USER</value>
    </data>
</entry>
```

**Сокращённый формат (без TTL):**

```xml
<entry key="user:roles:1" utype="RedisSet">
    <value>ROLE_ADMIN</value>
    <value>ROLE_USER</value>
</entry>
```

### ZSET (Sorted Set / Упорядоченные множества)

Каждая запись состоит из члена множества (`member`) и его числового балла (`score`), по которому
элементы автоматически сортируются. Представляется типом `RedisZSet`, а элементы — типом
`RedisZSetEntry`.

```xml
<entry key="leaderboard" utype="RedisEntry">
    <data type="RedisZSet">
        <value type="RedisZSetEntry">
            <member>player_A</member>
            <score type="BigDecimal">1500</score>
        </value>
        <value type="RedisZSetEntry">
            <member>player_B</member>
            <score type="BigDecimal">2100.50</score>
        </value>
    </data>
</entry>
```

**Сокращённый формат (без TTL):**

```xml
<entry key="leaderboard" utype="RedisZSet">
    <value type="RedisZSetEntry">
        <member>player_A</member>
        <score type="BigDecimal">1500</score>
    </value>
    <value type="RedisZSetEntry">
        <member>player_B</member>
        <score type="BigDecimal">2100.50</score>
    </value>
</entry>
```

**В diff** упорядоченное множество пишется по членам: член идентифицирует элемент, поэтому он
становится `key`, а балл — атрибутом `score`. Появившийся член — это `insert`, изменившийся балл —
`update`, исчезнувший член — `delete`:

```xml
<insert key="leaderboard" type="RedisZSet">
    <player_A score="1500"/>
    <player_C score="1200"/>
</insert>
```

Изменение балла не создаёт новый элемент, поэтому обновление множества — одна строка. Для этого
член должен выражаться идентификатором: строку задаёт
[`addDiffKeyPresenter`](../Storage-Diff-Extensibility.md#ключ-записи--adddiffkeypresenter), вернувший скаляр, а
структурный объект пишется объектным ключом по умолчанию, и его проекцию можно уточнить тем же
методом. Тогда member пишется структурой, а балл остаётся атрибутом:

```xml
<insert key="leaderboard" type="RedisZSet">
    <insert score="1500">
        <key type="MarketInstrumentKey">
            <exchange>MOEX</exchange>
            <ticker>SBER</ticker>
        </key>
    </insert>
</insert>
```

Если члены — большие значения без идентификатора, множество лучше представить коллекцией:
зарегистрировать презентер `RedisZSet`, возвращающий само множество, и презентер
`RedisZSetEntry`, у которого тело — член, а атрибут — балл. Тогда фреймворк прочитает значение как
обычную коллекцию и напишет `<insert score="1500">{большой член}</insert>`.

### STREAM (Стримы)

Стримы хранят упорядоченную последовательность записей, где у каждой записи есть уникальный ID
(например, `1716530000000-0`) и набор полей. Представляется типом `RedisStream`, элементы —
`RedisStreamEntry`.

```xml
<entry key="orders:stream" utype="RedisEntry">
    <data type="RedisStream">
        <value type="RedisStreamEntry" id="1716530000000-0">
            <fields>
                <key>order_id</key>
                <value>ORD-99</value>
                <key>amount</key>
                <value>450.00</value>
            </fields>
        </value>
    </data>
</entry>
```

**Сокращённый формат (без TTL):**

```xml
<entry key="orders:stream" utype="RedisStream">
    <value type="RedisStreamEntry" id="1716530000000-0">
        <fields>
            <key>order_id</key>
            <value>ORD-99</value>
            <key>amount</key>
            <value>450.00</value>
        </fields>
    </value>
</entry>
```

### HyperLogLog (HLL)

Поскольку HyperLogLog используется для вероятностного подсчёта уникальных элементов, фреймворк для
удобства тестирования сохраняет его как обычное уникальное множество элементов. Представляется типом
`RedisHyperLogLog`.

```xml
<entry key="unique:visitors" utype="RedisEntry">
    <data type="RedisHyperLogLog">
        <value>user_ip_1</value>
        <value>user_ip_2</value>
    </data>
</entry>
```

**Сокращённый формат (без TTL):**

```xml
<entry key="unique:visitors" utype="RedisHyperLogLog">
    <value>user_ip_1</value>
    <value>user_ip_2</value>
</entry>
```

---
[← На главную](../README.md)
