TestSetupModule
===============

Обзор
-----

`TestSetupModule` --- это центральный модуль конфигурации фреймворка для регистрации типов, алиасов, кастомных
сериализаторов и других настроек сериализации/десериализации тестов.

Назначение
----------

Модуль решает следующие задачи:

| Задача                            | Описание                                                                           |
|-----------------------------------|------------------------------------------------------------------------------------|
| **Регистрация полиморфных типов** | Связывает Java-классы с логическими именами для XML/JSON                           |
| **Настройка алиасов**             | Позволяет использовать короткие имена вместо полных квалифицированных имён классов |
| **Кастомизация Jackson**          | Добавление модулей, фильтров, миксинов для сериализации                            |
| **Настройка клонирования**        | Конфигурация поведения клонирования для специфических типов                        |
| **Кастомное сравнение**           | Регистрация собственных предикатов равенства для типов                             |

Создание модуля
---------------

```
java  
@Bean  
TestSetupModule testSetupModule() {  
    return new TestSetupModule()  
        .addSubTypes(MyDto.class, "MyDto")  
        .addAlias(ByteArrayResource.class, "Resource")  
        .addJacksonModule(new MyCustomModule());  
}  

```

Методы конфигурации
-------------------

### Регистрация типов

| Метод                                                    | Описание                                                  | Пример                                                           |
|----------------------------------------------------------|-----------------------------------------------------------|------------------------------------------------------------------|
| `addSubTypes(Class<?> subType, String name)`             | Регистрирует подтип с явным именем                        | `.addSubTypes(MyDto.class, "MyDto")`                             |
| `addSubTypes(Class<?>... classes)`                       | Регистрирует подтипы с именами по умолчанию (simple name) | `.addSubTypes(MyDto1.class, MyDto2.class)`                       |
| `addSubTypes(String packageName)`                        | Сканирует пакет и регистрирует все классы как подтипы     | `.addSubTypes("com.example.dto")`                                |
| `addSubTypes(String packageName, Set<Class<?>> exclude)` | Сканирует пакет с исключениями                            | `.addSubTypes("com.example.dto", Set.of(Excluded.class))`        |
| `addSubTypes(JsonSubTypes jsonSubTypes)`                 | Регистрирует подтипы из аннотации `@JsonSubTypes`         | `.addSubTypes(Selector.class.getAnnotation(JsonSubTypes.class))` |

### Алиасы типов

| Метод                                      | Описание                              | Пример                                           |
|--------------------------------------------|---------------------------------------|--------------------------------------------------|
| `addAlias(Class<?> subType, String alias)` | Добавляет альтернативное имя для типа | `.addAlias(ByteArrayResource.class, "Resource")` |

### Jackson модули и фильтры

| Метод                                                             | Описание                                 | Пример                                    |
|-------------------------------------------------------------------|------------------------------------------|-------------------------------------------|
| `addJacksonModule(com.fasterxml.jackson.databind.Module module)`  | Добавляет Jackson-модуль                 | `.addJacksonModule(new JavaTimeModule())` |
| `addJacksonFilter(String id, PropertyFilter filter)`              | Добавляет Jackson-фильтр                 | `.addJacksonFilter("filterId", myFilter)` |
| `setHandlerInstantiator(HandlerInstantiator handlerInstantiator)` | Устанавливает обработчик инстанцирования | `.setHandlerInstantiator(myHandler)`      |

### Настройка клонирования

| Метод                                                               | Описание                        | Пример                                                                      |
|---------------------------------------------------------------------|---------------------------------|-----------------------------------------------------------------------------|
| `clonerFieldAction(Class<?> type, String field, CopyAction action)` | Действие для конкретного поля   | `.clonerFieldAction(Throwable.class, "stackTrace", CopyAction.ORIGINAL)`    |
| `clonerFieldAction(Field field, CopyAction action)`                 | Действие для поля через反射       | `.clonerFieldAction(field, CopyAction.ORIGINAL)`                            |
| `clonerTypeAction(Class<?> type, CopyAction action)`                | Действие для всех полей типа    | `.clonerTypeAction(SecureRandom.class, CopyAction.ORIGINAL)`                |
| `clonerTypeAction(Predicate<Class<?>> type, CopyAction action)`     | Действие для типов по предикату | `.clonerTypeAction(Throwable.class::isAssignableFrom, CopyAction.ORIGINAL)` |

### Кастомное сравнение

| Метод                                                                       | Описание                                 | Пример                                                      |
|-----------------------------------------------------------------------------|------------------------------------------|-------------------------------------------------------------|
| `addEqualsForType(Class<T> type, BiPredicate<? super T, ? super T> equals)` | Регистрирует предикат равенства для типа | `.addEqualsForType(SecureRandom.class, (sr1, sr2) -> true)` |

Пример конфигурации
-------------------

### Базовая конфигурация

```
java  
@Configuration  
public class TestConfig {  
    
    @Bean  
    TestSetupModule testSetupModule() {  
        return new TestSetupModule()  
            // Регистрация DTO  
            .addSubTypes(MyDto.class, "MyDto")  
            .addSubTypes(AnotherDto.class, "AnotherDto")  
            
            // Алиасы  
            .addAlias(ByteArrayResource.class, "Resource")  
            
            // Jackson модули  
            .addJacksonModule(new JavaTimeModule());  
    }  
}  

```

### Расширенная конфигурация

```
java  
@Configuration  
public class AdvancedTestConfig {  
    
    @Bean  
    TestSetupModule testSetupModule() {  
        return new TestSetupModule()  
            // Сканирование пакета  
            .addSubTypes("com.example.dto", Set.of(ExcludedDto.class))  
            
            // Регистрация из аннотации  
            .addSubTypes(Selector.class.getAnnotation(JsonSubTypes.class))  
            
            // Jackson модули  
            .addJacksonModule(new JavaTimeModule())  
            .addJacksonModule(new StoreLocationModule())  
            
            // Миксины  
            .addJacksonModule(new SimpleModule()  
                .setMixInAnnotation(Throwable.class, ThrowableMixIn.class)  
                .setMixInAnnotation(SecureRandom.class, SecureRandomMixIn.class))  
            
            // Настройка клонирования  
            .clonerTypeAction(Throwable.class::isAssignableFrom, CopyAction.ORIGINAL)  
            .clonerTypeAction(SecureRandom.class, CopyAction.ORIGINAL)  
            
            // Кастомное сравнение  
            .addEqualsForType(SecureRandom.class, (sr1, sr2) -> true);  
    }  
}  

```

### Пример с клонированием поля через反射

```
java  
@Configuration  
public class TestConfig {  
    
    @Bean  
    TestSetupModule testSetupModule() throws NoSuchFieldException {  
        Field stackTraceField = Throwable.class.getDeclaredField("stackTrace");  
        
        return new TestSetupModule()  
            // Настройка клонирования для конкретного поля  
            .clonerFieldAction(stackTraceField, CopyAction.ORIGINAL)  
            
            // Настройка клонирования для всех полей типа  
            .clonerTypeAction(Throwable.class, CopyAction.ORIGINAL);  
    }  
}  

```

Предварительно зарегистрированные типы
--------------------------------------

Фреймворк уже регистрирует следующие распространённые типы через `CommonFormatConfig`:

### Типы тестов

| Класс           | Имя         |
|-----------------|-------------|
| `TestContainer` | `Container` |
| `TestCase`      | `Case`      |
| `TestPart`      | `Part`      |

### Примитивы и обёртки

| Класс                                                     | Имя         |
|-----------------------------------------------------------|-------------|
| `String`, `Integer`, `Long`, `Double`, `Float`, `Boolean` | Простое имя |
| `BigDecimal`, `BigInteger`                                | Простое имя |
| `boolean`, `int`, `long`, `double`, `float`               | Простое имя |

### Коллекции

| Класс                                   | Имя                |
|-----------------------------------------|--------------------|
| `ArrayList`, `LinkedHashMap`, `TreeMap` | Простое имя        |
| `LinkedHashSet`, `TreeSet`              | Простое имя        |
| `byte[]`, `int[]`, `long[]`, `double[]` | Простое имя с `[]` |

### Дата и время

| Класс                                     | Имя         |
|-------------------------------------------|-------------|
| `LocalTime`, `LocalDate`, `LocalDateTime` | Простое имя |
| `ZonedDateTime`, `Instant`, `Date`        | Простое имя |

### Специальные типы

| Класс                 | Имя/Алиас                                  |
|-----------------------|--------------------------------------------|
| `SecureRandom`        | `SecureRandom`                             |
| `Resource`            | `Resource` (алиас для `ByteArrayResource`) |
| `UUID`, `Class`       | Простое имя                                |
| `Throwable` и подтипы | Простое имя                                |

💡 **Совет:** Не нужно регистрировать эти типы повторно --- они уже доступны для использования в тестах.

Как это работает
----------------

### 1. Регистрация типа

```
java  
// В конфигурации  
.addSubTypes(MyDto.class, "MyDto")  

// В XML  
<response type="MyDto">  
    <id>1</id>  
    <name>test</name>  
</response>  

// В JSON  
{  
    "type": "MyDto",  
    "id": 1,  
    "name": "test"  
}  

```

### 2. Сериализация

При сериализации фреймворк использует **короткое имя** типа, зарегистрированное в `TestSetupModule`:

```
java  
// Если MyDto.class зарегистрирован как "MyDto"  
// В XML/JSON всегда будет type="MyDto"  
// Полное квалифицированное имя не используется  

```

### 3. Десериализация

При десериализации фреймворк ищет тип по короткому имени:

```
1. Jackson читает атрибут/поле "type": "MyDto"  
2. TypeResolverFactory.resolve("MyDto") → MyDto.class  
3. Jackson десериализует в MyDto.class  

```

⚠️ **Важно:** Если тип не зарегистрирован, десериализация завершится ошибкой. Использование полного квалифицированного
имени в XML/JSON не работает --- фреймворк всегда ожидает короткое имя.

Взаимодействие с другими компонентами
-------------------------------------

| Компонент                  | Взаимодействие                                                  |
|----------------------------|-----------------------------------------------------------------|
| `TypeResolverFactory`      | Собирает данные из всех `TestSetupModule`                       |
| `TestTypeResolverBuilder`  | Использует `TypeResolverFactory` для разрешения типов           |
| `SharedTypeNameIdResolver` | Создаётся `TypeResolverFactory` на основе настроек модуля       |
| `IntegrationTestConfig`    | Импортирует конфигурацию, создаёт `Cloner` с настройками модуля |
| `XmlConfig` / `JsonConfig` | Используют `TestTypeResolverBuilder` для полиморфизма           |
| `CommonFormatConfig`       | Предоставляет базовый `TestSetupModule` со стандартными типами  |

CopyAction для клонирования
---------------------------

| Значение                  | Описание                                          |
|---------------------------|---------------------------------------------------|
| `CopyAction.ORIGINAL`     | Использовать оригинальный объект (не клонировать) |
| `CopyAction.DEEP_COPY`    | Глубокое клонирование                             |
| `CopyAction.SHALLOW_COPY` | Поверхностное клонирование                        |

Частые проблемы
---------------

### Конфликт имён типов

**Причина:** Два типа зарегистрированы с одинаковым логическим именем.  
**Решение:**

```
java  
// ❌ Ошибка: два типа с именем "Dto"  
.addSubTypes(FirstDto.class, "Dto")  
.addSubTypes(SecondDto.class, "Dto")  

// ✅ Правильно: уникальные имена  
.addSubTypes(FirstDto.class, "FirstDto")  
.addSubTypes(SecondDto.class, "SecondDto")  

```

### Тип не найден при десериализации

**Причина:** Тип не зарегистрирован в `TestSetupModule`.  
**Решение:**

```
java  
// Добавить регистрацию типа  
.addSubTypes(MyMissingType.class, "MyMissingType")  

// ❌ Не работает: использование полного имени в XML  
<response type="com.example.MyMissingType">...</response>  

```

⚠️ **Важно:** Фреймворк всегда использует короткие имена типов. Полное квалифицированное имя в XML/JSON не
поддерживается.

### Клонирование не работает для типа

**Причина:** Не настроено действие клонирования для типа.  
**Решение:**

```
java  
// Настроить клонирование  
.clonerTypeAction(MyType.class, CopyAction.DEEP_COPY)  

// Или для иерархии  
.clonerTypeAction(MyBaseType.class::isAssignableFrom, CopyAction.DEEP_COPY)  

```

Рекомендации
------------

1. **Регистрируйте все полиморфные типы** --- если тип используется в `response` или `request` с атрибутом `type`, он
   должен быть зарегистрирован.
2. **Проверяйте уникальность имён** --- фреймворк выбросит исключение при дублировании имени типа.
3. **Используйте сканирование пакетов для DTO** --- вместо ручной регистрации каждого типа:

   ```
   java  
   .addSubTypes("com.example.dto")  

   ```

4. **Настраивайте клонирование для типов с состоянием** --- особенно для `Throwable`, `Resource`, `InputStream`.
5. **Используйте алиасы для стандартных типов** --- если нужно использовать альтернативное имя для типа, уже
   зарегистрированного в `CommonFormatConfig`.  
