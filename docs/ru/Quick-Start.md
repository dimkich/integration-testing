Быстрый старт
=============

Шаг 1: Добавьте зависимость
---------------------------

```
xml  
<!-- pom.xml -->  
<dependency>  
    <groupId>io.github.dimkich</groupId>  
    <artifactId>integration-testing</artifactId>  
    <version>0.4.0</version>  
    <scope>test</scope>  
</dependency>  

```

Шаг 2: Создайте тестируемый сервис
----------------------------------

```
java  
package com.example.service;  

import org.springframework.stereotype.Service;  

@Service  
public class GreetingService {  
    
    public String greet(String name) {  
        return "Hello, " + name + "!";  
    }  
    
    public Integer add(Integer a, Integer b) {  
        return a + b;  
    }  
}  

```

Шаг 3: Создайте файл теста
--------------------------

Создайте файл `src/test/resources/tests/greeting.xml`:

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<!-- @formatter:off -->  
<test type="Container">  
    <test type="Case" name="приветствие">  
        <bean>greetingService</bean>  
        <method>greet</method>  
        <request>  
            <value>Мир</value>  
        </request>  
        <response>Привет, Мир!</response>  
    </test>  
    <test type="Case" name="сложение">  
        <bean>greetingService</bean>  
        <method>add</method>  
        <request>  
            <value type="Integer">10</value>  
            <value type="Integer">20</value>  
        </request>  
        <response type="Integer">30</response>  
    </test>  
</test>  

```

> ⚠️ **Важно:**
>
> * `type="String"` **не указывается** --- строка является типом по умолчанию
> * Если `<response>` содержит простой текст без `type`, он всегда трактуется как `String`
* `type` добавляется только для не-строковых типов: `Integer`, `Boolean`, `BigDecimal` и т.д.

Шаг 4: Создайте тестовый класс
------------------------------

```
java  
package com.example;  

import io.github.dimkich.integration.testing.*;  
import org.junit.jupiter.api.DynamicNode;  
import org.junit.jupiter.api.TestFactory;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.test.context.SpringBootTest;  

import java.util.stream.Stream;  

@IntegrationTesting  
@SpringBootTest  
public class GreetingIntegrationTest {  
    
    private final DynamicTestBuilder dynamicTestBuilder;  
    
    @Autowired  
    public GreetingIntegrationTest(DynamicTestBuilder dynamicTestBuilder) {  
        this.dynamicTestBuilder = dynamicTestBuilder;  
    }  
    
    @TestFactory  
    Stream<DynamicNode> tests() throws Exception {  
        return dynamicTestBuilder.build("tests/greeting.xml");  
    }  
}  

```

💡 **Примечание:** Аннотация `@IntegrationTesting` автоматически импортирует `IntegrationTestConfig`, поэтому не нужно указывать её в `@SpringBootTest(classes = ...)`.

Шаг 5: Запустите тест
---------------------

```
bash  
mvn test -Dtest=GreetingIntegrationTest  

```

Что происходит
--------------

| Этап |                                    Описание                                    |
|------|--------------------------------------------------------------------------------|
| 1    | `@IntegrationTesting` активирует фреймворк и загружает `IntegrationTestConfig` |
| 2    | `DynamicTestBuilder` читает `greeting.xml`                                     |
| 3    | Парсит XML в иерархию `Container` → `Case`                                     |
| 4    | Находит Spring-бин `greetingService`                                           |
| 5    | Вызывает метод `greet("Мир")`                                                  |
| 6    | Сравнивает результат с `<response>`                                            |
| 7    | Отчитывается об успехе или ошибке в JUnit                                      |

Структура теста
---------------

```
test type="Container" (корень, обязателен)  
├── test type="Container" (вложенный контейнер, опционально)  
│   └── test type="Case" (тест-кейс)  
│       └── test type="Part" (часть кейса, опционально)  
└── test type="Case" (тест-кейс)  
    ├── bean: имя Spring-бина  
    ├── method: имя метода  
    ├── request: аргументы метода (список <value>)  
    └── response: ожидаемый результат (String по умолчанию)  

```

Пример с вложенностью
---------------------

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<!-- @formatter:off -->  
<test type="Container">  
    <test type="Container" name="группа1">  
        <test type="Case" name="сценарий1">  
            <bean>greetingService</bean>  
            <method>greet</method>  
            <request>  
                <value>Тест</value>  
            </request>  
            <response>Привет, Тест!</response>  
        </test>  
        <test type="Case" name="сценарий2">  
            <test type="Part" name="шаг1">  
                <bean>greetingService</bean>  
                <method>add</method>  
                <request>  
                    <value type="Integer">5</value>  
                    <value type="Integer">5</value>  
                </request>  
                <response type="Integer">10</response>  
            </test>  
            <test type="Part" name="шаг2">  
                <bean>greetingService</bean>  
                <method>add</method>  
                <request>  
                    <value type="Integer">10</value>  
                    <value type="Integer">10</value>  
                </request>  
                <response type="Integer">20</response>  
            </test>  
        </test>  
    </test>  
    
    <!-- Отключённый тест -->  
    <test type="Case" name="отключено" disabled="true">  
        <bean>greetingService</bean>  
        <method>greet</method>  
        <request>  
            <value>Пропуск</value>  
        </request>  
    </test>  
</test>  

```

Типы для request и response
---------------------------

|   Java-тип   |                XML формат                 |         `type` атрибут          |
|--------------|-------------------------------------------|---------------------------------|
| `String`     | `<value>текст</value>`                    | **не требуется** (по умолчанию) |
| `Integer`    | `<value type="Integer">1</value>`         | требуется                       |
| `Long`       | `<value type="Long">1</value>`            | требуется                       |
| `Boolean`    | `<value type="Boolean">true</value>`      | требуется                       |
| `BigDecimal` | `<value type="BigDecimal">1.23</value>`   | требуется                       |
| `null`       | `<value xmlns:xsi="..." xsi:nil="true"/>` | не требуется                    |

💡 **Правило:** Если `type` не указан, фреймворк считает значение строкой (`String`).

Хуки (опционально)
------------------

Добавьте в конфигурацию для логирования до/после теста:

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    BeforeTest beforeTest() {  
        return test -> System.out.println("Before: " + test.getName());  
    }  
    
    @Bean  
    AfterTest afterTest() {  
        return test -> System.out.println("After: " + test.getName());  
    }  
}  

```

Частые проблемы
---------------

### Тест падает с ошибкой сравнения

**Причина:** Результат не совпадает с ожидаемым.  
**Решение:**

* Исправьте `<response>` в XML-файле теста
* Проверьте типы данных в `<request>` (для не-строковых типов указывайте `type="..."`)

### Тест пропускается без ошибок

**Причина:** Атрибут `disabled="true"` на тесте или родителе.  
**Решение:** Уберите `disabled` или проверьте наследование состояния.

### `Method not found in bean`

**Причина:** Метод не найден по имени и сигнатуре.  
**Решение:**

* Проверьте имя метода в `<method>`
* Убедитесь, что типы в `<request>` соответствуют параметрам метода
* Для не-строковых типов обязательно указывайте `type="..."`

### Ошибка при запуске тестов

**Причина:** Отсутствует аннотация `@SpringBootTest` на тестовом классе.  
**Решение:** Добавьте `@SpringBootTest` на класс теста (аннотация `@IntegrationTesting` не заменяет `@SpringBootTest`).