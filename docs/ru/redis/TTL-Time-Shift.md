Тестирование времени жизни (TTL) и сдвигов времени
====================================================

[English version](../../en/redis/TTL-Time-Shift.md)

Тестирование логики устаревания данных (например, сессий пользователей, временных одноразовых кодов
авторизации или кэшей) часто вызывает сложности. Если ключ записывается в Redis со временем жизни (TTL)
в 60 секунд, тест не может ждать минуту реального времени — это замедлит общую сборку проекта.

Фреймворк предоставляет встроенную поддержку виртуального времени и автоматической очистки просроченных
данных, что позволяет моментально проверять удаление ключей «на лету».

Проблема времени в Testcontainers и её решение
----------------------------------------------

Когда вы используете виртуальное время (аннотацию `@MockJavaTime` на тестовом классе и инициализатор
`DateTimeInit` в XML), фреймворк переводит стрелки часов только внутри виртуальной машины JVM вашего
теста.

Однако реальный сервер Redis (запущенный в Docker-контейнере или как embedded-процесс) работает снаружи
JVM и продолжает жить по реальному системному времени своего контейнера. Он ничего не знает о том, что
в вашем тесте время сдвинулось вперёд.

### Как фреймворк решает эту проблему?

При каждом сдвиге времени в XML-тесте (например, через `addDuration="PT10S"`):

1. Фреймворк обновляет внутренние виртуальные часы теста.
2. Фоновый анализатор обходит все ключи в локальном зеркале базы данных и вычисляет, у каких ключей
   относительный TTL стал меньше или равен нулю относительно нового виртуального времени.
3. Для всех просроченных ключей фреймворк мгновенно отправляет в реальный Redis команды на их физическое
   удаление (`DEL` для ключей или `HDEL` для отдельных полей хэшей).
4. **Результат:** Реальная база данных Redis синхронизируется с вашим виртуальным временем. Если ваше
   приложение попытается прочитать этот ключ в этот же момент времени, Redis вернёт `null` (ключ не
   найден).

Управление временем в XML (`DateTimeInit`)
------------------------------------------

Управлять временем можно на любом уровне иерархии тестов с помощью элемента инициализации `DateTimeInit`.

### Вариант А: Установка абсолютного времени

Используется в корневом контейнере, чтобы зафиксировать единую стартовую точку для всех тестов.

```xml
<init type="dateTimeInit" dateTime="2026-01-01T12:00:00Z" />
```

### Вариант Б: Относительный сдвиг времени (Time Shift)

Используется внутри шагов теста (`TestPart`), чтобы симулировать течение времени.

```xml
<!-- Применяем сдвиг времени на 15 секунд только для шагов текущего кейса -->
<init type="dateTimeInit" applyTo="TestPart" addDuration="PT15S"/>
```

Формат длительности `addDuration` следует стандарту ISO-8601:

| Пример    | Описание        |
|-----------|-----------------|
| `PT15S`   | 15 секунд       |
| `PT5M`    | 5 минут         |
| `PT2H30M` | 2 часа 30 минут |
| `P1D`     | 1 день          |

Время жизни на уровне полей хэша (`HEXPIRE`)
--------------------------------------------

Начиная с версии Redis 7.4, поддерживается установка TTL для отдельных полей внутри одного хэша
(команды `HEXPIRE` / `HPEXPIREAT`).

В XML-тесте вы можете задать время жизни индивидуально для каждого поля хэша. При сдвиге времени
фреймворк удалит из хэша только те поля, у которых истёк TTL, оставив остальные нетронутыми.

### Как описать хэш с TTL полей в XML:

```xml
<entry key="user:session:active" type="RedisEntry">
    <data type="RedisHash">
        <!-- Поле без TTL (вечное) -->
        <entry>
            <key>username</key>
            <value type="RedisEntry">
                <data>Ivan</data>
            </value>
        </entry>
        <!-- Поле со своим временем жизни (30 секунд) -->
        <entry>
            <key>one_time_token</key>
            <value type="RedisEntry">
                <data>token_9941</data>
                <ttl>PT30S</ttl> <!-- Удалится только это поле через 30 секунд -->
            </value>
        </entry>
    </data>
</entry>
```

Полноценный практический сценарий тестирования TTL
---------------------------------------------------

Ниже представлен готовый XML-сценарий для копирования, демонстрирующий тестирование вытеснения ключа
по времени.

### Описание сценария

1. **Шаг 1:** Записываем ключ `user:session:temp` со временем жизни 15 секунд. Проверяем его появление
   и TTL в базе.
2. **Шаг 2:** Переводим время вперёд на 10 секунд. Ключ должен остаться в Redis, но его TTL должен
   уменьшиться до 5 секунд.
3. **Шаг 3:** Переводим время вперёд ещё на 10 секунд (суммарно прошло 20 секунд с момента записи).
   Ключ должен физически удалиться из Redis, а попытка его прочитать должна вернуть пустоту (`null`).

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- @formatter:off -->
<test type="Container">
    <!-- Устанавливаем фиксированное стартовое время для всего файла -->
    <init type="DateTimeInit" dateTime="2026-05-24T12:00:00.000Z" />
    <init type="KeyValueStorageInit" name="redissonConnectionFactory" clear="true" />

    <test type="Case" name="Тестирование вытеснения сессии по истечении времени">
        <!-- Настраиваем сдвиг времени на 10 секунд на каждом шаге (Part) -->
        <init type="DateTimeInit" applyTo="TestPart" addDuration="PT10S"/>

        <test type="Part" name="1. Запись сессии с TTL 15 секунд">
            <bean>redisStringFacade</bean>
            <method>setEx</method>
            <request>user:session:temp</request>
            <request>ACTIVE_SESSION</request>
            <request type="Long">15</request>
            <response />
            
            <!-- Ожидаем появление ключа с TTL 15 секунд -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="added">
                        <key>user:session:temp</key>
                        <value type="RedisEntry">
                            <data>ACTIVE_SESSION</data>
                            <ttl>PT15S</ttl>
                        </value>
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>

        <test type="Part" name="2. Сдвиг времени на 10 секунд (ключ существует, TTL уменьшился)">
            <!-- Время сдвинулось на +10s (прошло 10s от старта). Ключ живет еще 5s -->
            <bean>redisStringFacade</bean>
            <method>get</method>
            <request>user:session:temp</request>
            <response>ACTIVE_SESSION</response>
            
            <!-- Проверяем, что в базе данных TTL уменьшился до 5 секунд -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="changed">
                        <key>user:session:temp</key>
                        <value type="EntriesObjectKeyObjectValue">
                            <entry change="changed">
                                <key>ttl</key>
                                <value type="PeriodDuration">PT5S</value>
                            </entry>
                        </value>
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>

        <test type="Part" name="3. Сдвиг времени еще на 10 секунд (ключ должен удалиться)">
            <!-- Время сдвинулось еще на +10s (прошло 20s от старта). Ключ полностью истек. -->
            <bean>redisStringFacade</bean>
            <method>get</method>
            <request>user:session:temp</request>
            <!-- Метод get должен вернуть null (профиль удален) -->
            <response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
            
            <!-- Проверяем, что ключ физически удалился из хранилища -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="deleted">
                        <key>user:session:temp</key>
                        <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>
    </test>
</test>
```

---
[← На главную](../README.md)
