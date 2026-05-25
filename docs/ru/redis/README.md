Интеграционное тестирование Redis
==================================

[English version](../../en/redis/README.md)

Этот модуль позволяет тестировать работу вашего приложения с базой данных Redis. Вы можете описывать
сценарии тестов в XML или JSON файлах: записывать начальные данные в Redis перед тестом, вызывать методы
приложения и проверять, как изменились данные в базе после этого.

Как это работает?
-----------------

Вам не нужно вручную писать проверки кода (ассерты) на Java. Фреймворк делает всё автоматически:

1. **Подготовка данных:** Перед началом теста фреймворк записывает в реальный Redis те данные, которые
   вы указали в блоке `<init>`.
2. **Выполнение теста:** Запускается ваш тест (например, вызывается метод сервиса или отправляется
   сообщение в очередь).
3. **Слежение за базой:** Специальный фоновый механизм фреймворка мгновенно подхватывает любые изменения,
   которые ваше приложение делает в Redis (создание, изменение, удаление ключей или изменение их времени
   жизни TTL).
4. **Проверка результатов:** В конце теста фреймворк сравнивает реальное состояние базы данных с тем,
   что вы описали в блоке `<dataStorageDiff>`, и показывает разницу в виде удобного визуального
   сравнения (Diff), если что-то пошло не так.

Подключение к проекту
---------------------

Чтобы тесты с Redis начали работать, достаточно добавить одну аннотацию над вашим тестовым классом в Java:

```java
package com.example.redis;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.redis.EnableTestRedis;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

@EnableTestRedis // <-- Эта аннотация включает поддержку тестов Redis
@SpringBootTest
public class UserRedisIntegrationTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> redisTests() throws Exception {
        // Указываем путь к нашему XML-файлу с тестами
        return dynamicTestBuilder.build("tests/redis-user.xml");
    }
}
```

Быстрый старт: Пишем первый тест
---------------------------------

Создайте файл `src/test/resources/tests/redis-user.xml`. В этом примере мы:

* Очистим базу данных перед тестом.
* Запишем сессию пользователя со значением `ACTIVE`.
* Проверим, что ключ действительно появился в базе.
* Вызовем метод удаления и убедимся, что ключ пропал из Redis.

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- @formatter:off -->
<test type="Container">
    <!-- Очищаем базу данных Redis перед запуском теста -->
    <init type="keyValueStorageInit" name="redissonConnectionFactory" clear="true" />

    <test type="Case" name="Тестирование сессий в Redis">
        
        <test type="Part" name="Шаг 1: Создание сессии">
            <!-- Вызываем метод set у бина redisStringFacade -->
            <bean>redisStringFacade</bean>
            <method>set</method>
            <request>user:session:100</request>
            <request>ACTIVE</request>
            
            <!-- Проверяем, что в Redis добавился наш ключ -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="added">
                        <key>user:session:100</key>
                        <value type="RedisEntry">
                            <data>ACTIVE</data>
                        </value>
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>

        <test type="Part" name="Шаг 2: Проверка чтения сессии">
            <bean>redisStringFacade</bean>
            <method>get</method>
            <request>user:session:100</request>
            <!-- Метод должен вернуть ACTIVE -->
            <response>ACTIVE</response>
        </test>

        <test type="Part" name="Шаг 3: Удаление сессии">
            <bean>redisStringFacade</bean>
            <method>delete</method>
            <request>user:session:100</request>
            <!-- Метод удаления вернул true (успешно) -->
            <response type="Boolean">true</response>
            
            <!-- Проверяем, что ключ действительно удалился из Redis -->
            <dataStorageDiff type="MapStringKeyObjectValue">
                <redissonConnectionFactory type="EntriesObjectKeyObjectValue">
                    <entry change="deleted">
                        <key>user:session:100</key>
                        <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
                    </entry>
                </redissonConnectionFactory>
            </dataStorageDiff>
        </test>
        
    </test>
</test>
```

Поддерживаемые типы данных Redis
---------------------------------

Вы можете работать со всеми основными типами данных Redis. Вот как они записываются в XML:

| Тип Redis | Описание |
|-----------|----------|
| **String** (Строки) | Обычный текст |
| **Hash** (Хэши / Словари) | Набор полей и значений (как папки с файлами). Каждое поле может иметь своё время жизни (TTL) |
| **List** (Списки) | Список элементов, в котором важен порядок добавления |
| **Set** (Множества) | Набор уникальных элементов (без дубликатов) |
| **Sorted Set** (ZSet / Упорядоченные множества) | Элементы, у каждого из которых есть числовой балл (score), по которому они автоматически сортируются |
| **Stream** (Стримы) | Очередь сообщений, где у каждого сообщения есть свой ID (например, `1698400000000-0`) и набор полей |
| **HyperLogLog** (HLL) | Структура для быстрого подсчёта уникальных элементов (в тестах отображается как обычное уникальное множество) |

---
[← На главную](../README.md)
