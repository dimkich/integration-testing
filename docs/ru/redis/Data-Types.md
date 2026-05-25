Подготовка данных и Снимки базы
================================

[English version](../../en/redis/Data-Types.md)

Тестирование логики с Redis всегда состоит из двух шагов:

1. **Запись начального состояния (Seed):** С помощью блока `<init>` вы наполняете Redis тестовыми ключами.
2. **Проверка изменений (Assert):** С помощью блока `<dataStorageDiff>` вы описываете, какие ключи должны
   были добавиться, измениться или удалиться в базе после выполнения кода вашего приложения.

Решение проблемы двоеточий в ключах Redis
-----------------------------------------

В Redis принято разделять логические части ключа двоеточием, например: `user:profile:100`.
Однако в формате XML символ двоеточия зарезервирован для пространств имён (`namespace:tag`). Если вы
попытаетесь написать тег `<user:profile:100>`, XML-парсер выдаст ошибку синтаксиса.

Чтобы этого избежать, фреймворк использует специальный тип данных `LinkedHashMapStringObject`. Он
позволяет передавать имя ключа как обычную строку в атрибуте `key="..."` внутри тега `<entry>`. Это даёт
возможность использовать в ключах абсолютно любые символы, включая двоеточия:

```xml
<!-- Пример правильной записи ключа с двоеточием -->
<map type="LinkedHashMapStringObject">
    <entry key="user:profile:100">ACTIVE</entry>
</map>
```

Инициализация базы через `<init>`
---------------------------------

Секция инициализации описывается в начале контейнера или тест-кейса. Для Redis используется тип
инициализатора `keyValueStorageInit`:

```xml

<init type="keyValueStorageInit" name="имя_подключения" clear="true/false">
    <map type="LinkedHashMapStringObject">
        <!-- Ваши данные -->
    </map>
</init>
```

* `name` — имя подключения (соответствует имени бина `RedisConnectionFactory` в проекте, например,
  `redissonConnectionFactory`).
* `clear="true"` — полностью очистить базу перед тестом (выполняется команда `FLUSHALL` на сервере).
* `clear="false"` — не очищать базу (новые ключи допишутся или обновят существующие).

Инициализатор поддерживает два формата записи данных: **Сокращённый** и **Полный**.

### Сокращённый формат (Abbreviated Format)

Используется для быстрой записи простых строк, чисел или флагов, когда у ключей нет времени жизни (TTL).

В этом формате значение ключа записывается текстом прямо внутри тега `<entry>`:

```xml

<init type="keyValueStorageInit" name="redissonConnectionFactory" clear="true">
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

При полной записи значение ключа обязательно оборачивается в тег `<value type="RedisEntry">`, внутри
которого располагаются теги `<data>` (само значение) и `<ttl>` (длительность жизни):

```xml

<init type="keyValueStorageInit" name="redissonConnectionFactory" clear="true">
    <map type="LinkedHashMapStringObject">

        <!-- Пример строки с временем жизни 5 минут -->
        <entry key="temp:otp:code" type="RedisEntry">
            <data>9941</data>
            <ttl>PT5M</ttl> <!-- Формат ISO-8601: PT5M = 5 минут, PT30S = 30 секунд -->
        </entry>

    </map>
</init>
```

> **Совет:** Если TTL не требуется, используйте сокращённую запись с `utype` — примеры для каждого
> типа данных приведены ниже в соответствующих разделах.

Проверка изменений через `<dataStorageDiff>`
--------------------------------------------

Блок `<dataStorageDiff>` описывает, какие изменения произошли в Redis после работы вашего приложения.
Он строится на основе сравнения состояния «до» и «после» выполнения теста.

Каждая запись внутри диффа имеет атрибут `change`, который указывает характер изменений:

* `change="added"` — ключ или элемент был добавлен в пустую базу.
* `change="changed"` — значение существующего ключа или элемента изменилось.
* `change="deleted"` — ключ или элемент был полностью удалён.

Пример диффа для удаления ключа:

```xml
<dataStorageDiff type="MapStringKeyObjectValue">
    <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
        <!-- Указываем, что ключ был удален -->
        <entry change="deleted">
            <key>user:session:100</key>
            <!-- Обязательно передаем пустое значение nil для удаленного элемента -->
            <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
        </entry>
    </redissonConnectionFactory>
</dataStorageDiff>
```

XML-представление для всех структур данных Redis (Полный формат)
----------------------------------------------------------------

Ниже приведены готовые шаблоны для записи и сравнения любых типов данных Redis в полном формате.
Вы можете использовать их как в `<init>` (для подготовки), так и в `<dataStorageDiff>` (для проверки
результатов).

### HASH (Хэши / Таблицы)

В Redis хэш хранит набор полей и значений. В нашей системе он представляется типом `RedisHash`.
Каждое поле хэша является самостоятельным элементом `RedisEntry`, поэтому каждому полю хэша можно
задать свой собственный TTL (команда `HEXPIRE`).

```xml
<entry key="user:profile:100" type="RedisEntry">
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
<entry key="notifications:queue" type="RedisEntry">
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

Множества содержат только уникальные элементы. Элементы множества в XML автоматически сортируются по
алфавиту для стабильности сравнения в тестах. Представляются типом `RedisSet`.

```xml
<entry key="user:roles:1" type="RedisEntry">
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
<entry key="leaderboard" type="RedisEntry">
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

### STREAM (Стримы)

Стримы хранят упорядоченную последовательность записей, где у каждой записи есть уникальный ID
(например, `1716530000000-0`) и набор полей. Представляется типом `RedisStream`, элементы —
`RedisStreamEntry`.

```xml
<entry key="orders:stream" type="RedisEntry">
    <data type="RedisStream">
        <value type="RedisStreamEntry" id="1716530000000-0">
            <fields>
                <entry>
                    <key>order_id</key>
                    <value>ORD-99</value>
                </entry>
                <entry>
                    <key>amount</key>
                    <value>450.00</value>
                </entry>
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
            <entry>
                <key>order_id</key>
                <value>ORD-99</value>
            </entry>
            <entry>
                <key>amount</key>
                <value>450.00</value>
            </entry>
        </fields>
    </value>
</entry>
```

### HyperLogLog (HLL)

Поскольку HyperLogLog используется для вероятностного подсчёта уникальных элементов, фреймворк для
удобства тестирования сохраняет его как обычное уникальное множество элементов. Представляется типом
`RedisHyperLogLog`.

```xml
<entry key="unique:visitors" type="RedisEntry">
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
