Хуки и преобразователи (BeforeTest, AfterTest, TestConverter)
=============================================================

Обзор
-----

Фреймворк предоставляет три функциональных интерфейса для расширения поведения тестов:

| Интерфейс       | Когда вызывается                        | Назначение                                      | Выполняется для Container |
|-----------------|-----------------------------------------|-------------------------------------------------|---------------------------|
| `BeforeTest`    | Перед выполнением каждого теста         | Инициализация, настройка состояния, логирование | **Да**                    |
| `AfterTest`     | После выполнения каждого теста          | Очистка, освобождение ресурсов, пост-обработка  | **Да**                    |
| `TestConverter` | После выполнения теста, перед assertion | Преобразование данных теста для сравнения       | **Нет**                   |

Важные особенности
------------------

### Хуки не наследуются

`BeforeTest`, `AfterTest` и `TestConverter` --- это **Spring-бины** , которые:

* Выполняются **для каждого `Test`** (Container, Case, Part)
* **Не участвуют в наследовании настроек** (в отличие от `init`)
* Вызываются **всегда** , если тест не отключен (`disabled`)
* Порядок выполнения определяется порядком регистрации бинов в Spring-контексте

### TestConverter выполняется только для не-контейнеров

Важное отличие `TestConverter`:

| Тип теста   | BeforeTest | AfterTest | TestConverter |
|-------------|------------|-----------|---------------|
| `Container` | **Да**     | **Да**    | **Нет**       |
| `TestCase`  | **Да**     | **Да**    | **Да**        |
| `TestPart`  | **Да**     | **Да**    | **Да**        |

**Причина:** `TestConverter` предназначен для преобразования данных теста (`response`, `request` и т.д.), а
у `Container` этих данных нет --- он только группирует дочерние тесты.

BeforeTest
----------

### Описание

Функциональный интерфейс для выполнения кода **перед** каждым тестом.

```
java  
@FunctionalInterface  
public interface BeforeTest {  
    void before(Test test) throws Exception;  
}  

```

### Когда вызывается

```
TestExecutor.before()  
    └── beforeConsumer()  
        ├── initializationService.beforeTest()  
        └── BeforeTest.before() ← вызывается здесь (для Container, Case, Part)  

```

### Пример использования

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    BeforeTest logTestStart() {  
        return test -> System.out.println("Before: " + test.getName());  
    }  
    
    @Bean  
    BeforeTest clearDatabase() {  
        return test -> {  
            // Очистка БД перед каждым тестом (включая контейнеры)  
            jdbcTemplate.update("DELETE FROM users");  
            jdbcTemplate.update("DELETE FROM orders");  
        };  
    }  
}  

```

### Порядок выполнения

Если зарегистрировано несколько `BeforeTest`, они выполняются **последовательно** в порядке регистрации:

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    @Order(1)  
    BeforeTest first() {  
        return test -> System.out.println("First: " + test.getName());  
    }  
    
    @Bean  
    @Order(2)  
    BeforeTest second() {  
        return test -> System.out.println("Second: " + test.getName());  
    }  
}  

```

AfterTest
---------

### Описание

Функциональный интерфейс для выполнения кода **после** каждого теста.

```
java  
@FunctionalInterface  
public interface AfterTest {  
    void after(Test test) throws Exception;  
}  

```

### Когда вызывается

```
TestExecutor.after()  
    └── afterConsumer()  
        ├── initializationService.afterTest()  
        └── AfterTest.after() ← вызывается здесь (для Container, Case, Part)  

```

### Пример использования

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    AfterTest logTestEnd() {  
        return test -> System.out.println("After: " + test.getName());  
    }  
    
    @Bean  
    AfterTest logExecutionTime() {  
        return test -> {  
            long duration = test.getExecutionTime();  
            log.info("Test {} executed in {} ms", test.getName(), duration);  
        };  
    }  
}  

```

TestConverter
-------------

### Описание

Функциональный интерфейс для **преобразования данных теста** перед сравнением.

```
java  
@FunctionalInterface  
public interface TestConverter {  
    void convert(Test test) throws Exception;  
}  

```

### Когда вызывается

```
TestExecutor.runTest()  
    ├── test.executeMethod() или sendInboundMessage()  
    ├── waitCompletion.waitCompletion()  
    ├── pollMessages()  
    ├── testDataStorages.getMapDiff()  
    ├── TestConverter.convert() ← вызывается здесь (только для Case/Part)  
    └── assertion.assertTestsEquals()  

```

### Почему TestConverter не вызывается для Container

| Причина                       | Объяснение                                                |
|-------------------------------|-----------------------------------------------------------|
| Нет данных для преобразования | У `Container` нет `response`, `request`, `bean`, `method` |
| Container только группирует   | Его задача --- организация иерархии, не выполнение кода   |
| Преобразование бессмысленно   | Нечего нормализовать/маскировать на уровне контейнера     |

### Пример использования

```
java  
@Configuration  
static class Config {  
    
    @Bean  
    TestConverter trimStrings() {  
        return test -> {  
            // Удаляем пробелы из строковых ответов  
            if (test.getResponse() instanceof String) {  
                test.setResponse(((String) test.getResponse()).trim());  
            }  
        };  
    }  
    
    @Bean  
    TestConverter addReadableCustomData() {  
        return test -> {  
            // Добавляем читаемое представление в custom  
            if (test.getResponse() instanceof ComplexObject) {  
                ComplexObject obj = (ComplexObject) test.getResponse();  
                test.addCustom("readableSummary", obj.toSummaryString());  
                test.addCustom("itemCount", obj.getItems().size());  
            }  
        };  
    }  
}  

```

Полная схема жизненного цикла теста
-----------------------------------

```
┌─────────────────────────────────────────────────────────────┐  
│                    TestExecutor.before()                    │  
│  ├── test = expectedTest                                    │  
│  ├── assertion.setExpected(test)                            │  
│  ├── test.before()                                          │  
│  │   ├── initializationService.beforeTest()                 │  
│  │   └── BeforeTest.before() ← Для Container, Case, Part    │  
│  └── testDataStorages.setNewCurrentValue()                  │  
└─────────────────────────────────────────────────────────────┘  
                            ↓  
┌─────────────────────────────────────────────────────────────┐  
│              TestExecutor.runTest() (только Case/Part)      │  
│  ├── waitCompletion.start()                                 │  
│  ├── MockAnswer.enable()                                    │  
│  │   ├── sendInboundMessage() или executeMethod()           │  
│  │   └── waitCompletion.waitCompletion()                    │  
│  ├── pollMessages()                                         │  
│  ├── testDataStorages.getMapDiff()                          │  
│  ├── TestConverter.convert() ← ТОЛЬКО для Case/Part         │  
│  └── assertion.assertTestsEquals()                          │  
└─────────────────────────────────────────────────────────────┘  
                            ↓  
┌─────────────────────────────────────────────────────────────┐  
│                    TestExecutor.after()                     │  
│  ├── test.after()                                           │  
│  │   ├── initializationService.afterTest()                  │  
│  │   └── AfterTest.after() ← Для Container, Case, Part      │  
│  └── assertion.afterTests() (только для root, один раз)     │  
└─────────────────────────────────────────────────────────────┘  

```

Пример выполнения (из теста)
----------------------------

```
xml  
<test type="Container">  
    <test type="Container" name="#2">  
        <test type="Case" name="#2#2">  
            <test type="Part" name="#2#2#1">...</test>  
            <test type="Part" name="#2#2#2">...</test>  
        </test>  
    </test>  
</test>  

```

**Порядок выполнения хуков:**

```
before root          ← Container (корень)  
before #2            ← Container (вложенный)  
before #2#2          ← TestCase  
before #2#2#1        ← TestPart  
#2#2#1               ← Выполнение теста (только Case/Part)  
after #2#2#1         ← TestPart  
before #2#2#2        ← TestPart  
#2#2#2               ← Выполнение теста (только Case/Part)  
after #2#2#2         ← TestPart  
after #2#2           ← TestCase  
after #2             ← Container (вложенный)  
after root           ← Container (корень)  
afterTests root      ← Assertion (только для root)  

```

⚠️ **Важно:** Обратите внимание, что `BeforeTest` и `AfterTest` выполняются для **всех** уровней иерархии,
включая `Container`. `TestConverter` выполняется только для `TestCase` и `TestPart`.

Сравнение хуков
---------------

| Характеристика                     | BeforeTest             | AfterTest              | TestConverter                     |
|------------------------------------|------------------------|------------------------|-----------------------------------|
| **Когда вызывается**               | Перед тестом           | После теста            | После теста, перед assertion      |
| **Вызывается для Container**       | **Да**                 | **Да**                 | **Нет**                           |
| **Вызывается для Case**            | Да                     | Да                     | Да                                |
| **Вызывается для Part**            | Да                     | Да                     | Да                                |
| **Может модифицировать тест**      | Да (custom данные)     | Да (custom данные)     | **Да (response, request, и др.)** |
| **Вызывается для disabled тестов** | Нет                    | Нет                    | Нет                               |
| **Наследуется от родителей**       | Нет (бины)             | Нет (бины)             | Нет (бины)                        |
| **Порядок выполнения**             | По порядку регистрации | По порядку регистрации | По порядку регистрации            |
| **Типичное использование**         | Инициализация, setup   | Очистка, логирование   | Нормализация данных               |

Практический пример
-------------------

### Конфигурация с хуками и конвертерами

```
java  
package com.example.config;  

import io.github.dimkich.integration.testing.*;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
import org.springframework.jdbc.core.JdbcTemplate;  

@Configuration  
public class TestHooksConfig {  
    
    private final JdbcTemplate jdbcTemplate;  
    
    public TestHooksConfig(JdbcTemplate jdbcTemplate) {  
        this.jdbcTemplate = jdbcTemplate;  
    }  
    
    @Bean  
    BeforeTest logAllTests() {  
        // Выполняется для Container, Case, Part  
        return test -> System.out.println("Before: " + test.getName() +  
                                          " (type=" + test.getType() + ")");  
    }  
    
    @Bean  
    BeforeTest clearTables() {  
        // Выполняется для Container, Case, Part  
        return test -> {  
            jdbcTemplate.update("DELETE FROM orders");  
            jdbcTemplate.update("DELETE FROM users");  
        };  
    }  
    
    @Bean  
    AfterTest logAllTestsEnd() {  
        // Выполняется для Container, Case, Part  
        return test -> System.out.println("After: " + test.getName());  
    }  
    
    @Bean  
    TestConverter normalizeResponse() {  
        // Выполняется ТОЛЬКО для Case/Part  
        return test -> {  
            if (test.getResponse() instanceof String) {  
                test.setResponse(((String) test.getResponse()).trim());  
            }  
            
            // Добавляем читаемое представление в custom  
            if (test.getResponse() instanceof Order) {  
                Order order = (Order) test.getResponse();  
                test.addCustom("orderId", order.getId());  
                test.addCustom("itemCount", order.getItems().size());  
            }  
        };  
    }  
}  

```

Частые проблемы
---------------

### BeforeTest не вызывается для Container

**Причина:** Ожидается, что хуки не выполняются для контейнеров.  
**Решение:**

* Это **нормальное поведение** --- `BeforeTest` и `AfterTest` выполняются для **всех** типов тестов, включая `Container`
* Если хук не вызывается, проверьте регистрацию бина в Spring-контексте

### TestConverter вызывается для Container

**Причина:** Ожидается, что конвертер выполняется для всех тестов.  
**Решение:**

* `TestConverter` **не вызывается** для `Container` по дизайну
* У контейнеров нет данных (`response`, `request`) для преобразования
* Используйте `BeforeTest`/`AfterTest` для логики на уровне контейнера

### Хуки выполняются в неправильном порядке

**Причина:** Не задан порядок регистрации бинов.  
**Решение:**

* Используйте `@Order` для явного указания порядка выполнения
* Или зарегистрируйте хуки в нужном порядке в конфигурации

Рекомендации
------------

1. **BeforeTest** --- используйте для:
    * Очистки состояния (БД, файлы, кэш) --- выполняется для Container, Case, Part
    * Установки тестовых данных
    * Логирования начала теста (включая контейнеры)
2. **AfterTest** --- используйте для:
    * Очистки ресурсов --- выполняется для Container, Case, Part
    * Логирования результатов (включая контейнеры)
    * Отправки метрик/уведомлений
3. **TestConverter** --- используйте для:
    * Нормализации данных перед сравнением --- **только Case/Part**
    * Удаления нестабильных полей (timestamp, id)
    * Маскирования чувствительных данных
    * Добавления читаемых данных в `Test.custom`
4. **Помните:**
    * `BeforeTest` и `AfterTest` выполняются для **всех** типов тестов (Container, Case, Part)
    * `TestConverter` выполняется **только** для `TestCase` и `TestPart`

* Хуки --- это Spring-бины, они **не наследуются** , а выполняются для каждого теста  
