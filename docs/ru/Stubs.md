Стабы в интеграционных тестах (StubBean, StubConstruction, StubStaticMethods)
================================================================================

Обзор
-----

Фреймворк предоставляет три аннотации для настройки стабов на основе Mockito:

| Аннотация            | Назначение                          | Область применения                                     |
|----------------------|-------------------------------------|--------------------------------------------------------|
| `@StubBean`          | Создание стабов Spring-бинов        | Экземпляры бинов в контексте                           |
| `@StubConstruction`  | Создание стабов конструкторов       | Любые объекты, создаваемые через конструктор (не бины) |
| `@StubStaticMethods` | Создание стабов статических методов | Статические методы классов                             |

Все аннотации:

* Применяются на уровне тестового класса
* Поддерживают повторение (repeatable)
* Интегрируются с системой `StubCall` для конфигурации ожидаемых вызовов в XML

StubBean
------------

### Описание

Создаёт стаб или spy для **Spring-бина** по имени или типу.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(StubBean.List.class)
public @interface StubBean {
    String name() default "";                    // Имя бина в Spring-контексте  
    Class<?> stubClass() default Null.class;     // Тип бина (если name не указан)
    String stubClassName() default "";           // FQN типа (альтернатива stubClass)
    String[] methods() default {};               // Конкретные методы для стабирования
    boolean spy() default false;                 // Использовать spy вместо стаба
    boolean cloneArgsAndResult() default false;  // Глубокое клонирование аргументов/результатов  
}  

```

### Когда использовать

**Используйте `@StubBean` когда нужно стабить Spring-бин:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubBean(name = "paymentService")
public class OrderTest {  
    // paymentService будет заменён на стаб
}  

```

### Примеры использования

**Стаб бина по имени:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubBean(name = "paymentService")
public class OrderTest {  
    // paymentService будет заменён на стаб
}  

```

**Несколько стабов:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubBean(name = "emailService")
@StubBean(name = "smsService", spy = true)
public class NotificationTest {  
    // emailService --- стаб, smsService --- spy
}  

```

**Spy для бина по типу:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubBean(stubClass = UserService.class, spy = true)
public class UserTest {  
    // Все бины типа UserService будут spy  
}  

```

### Когда использовать spy

`spy = true` используется когда нужно **иногда выполнять реальный метод, а иногда нет** :

```
java  
@StubBean(name = "paymentService", spy = true)

```

**Типичные сценарии:**

* Выброс исключений в определённых условиях
* Частичное сохранение реальной логики
* Верификация вызовов с сохранением поведения

**Важно:** В стабах тоже можно задать конкретные методы через `methods = {...}`. Spy не нужен только для ограничения
подменяемых методов.

### Поведение

| Параметр                    | Значение по умолчанию              | Эффект                                               |
|-----------------------------|------------------------------------|------------------------------------------------------|
| `spy = false`               | Полный стаб                        | Методы не выполняются, если не заданы в `<stubCall>` |
| `spy = true`                | Spy-режим                          | Реальные методы выполняются, если не переопределены  |
| `cloneArgsAndResult = true` | Аргументы и результаты клонируются | Избегает побочных эффектов от общих ссылок           |

StubConstruction
-------------------

### Описание

Стабит вызовы конструкторов указанного класса.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(StubConstruction.List.class)
public @interface StubConstruction {
    String name() default "";                    // Логическое имя конфигурации  
    Class<?> stubClass() default Null.class;     // Класс для стабирования конструкторов
    String stubClassName() default "";           // FQN класса (альтернатива stubClass)
    String[] methods() default {};               // Сигнатуры конструкторов (опционально)  
    boolean spy() default false;                 // Spy для созданных экземпляров  
    boolean cloneArgsAndResult() default false;  // Клонирование аргументов/результатов  
}  

```

### Когда использовать

**Используйте `@StubConstruction` когда нужно стабить объекты, которые не являются Spring-бинами:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubConstruction(stubClass = ExternalApiClient.class)
public class ApiTest {  
    // new ExternalApiClient(...) вернёт стаб вместо реального объекта
}  

```

**Подходит для любого объекта, который создаётся через конструктор:**

* DTO и модели данных
* Клиенты внешних сервисов
* Утилитные классы
* Любые POJO, создаваемые через `new`

### Примеры использования

**Стабирование всех конструкторов класса:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubConstruction(stubClass = ExternalApiClient.class)
public class ApiTest {  
    // new ExternalApiClient(...) вернёт стаб вместо реального объекта
}  

```

**Spy для созданных экземпляров:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubConstruction(stubClass = Order.class, spy = true)
public class OrderTest {  
    // new Order(...) создаст реальный объект, но обернёт в spy  
}  

```

### Когда использовать stubClassName

`stubClassName` полезен когда класс **недоступен напрямую** :

* Private классы
* Внутренние классы
* Классы без прямого доступа в classpath теста

```
java  
@StubConstruction(stubClassName = "com.example.internal.PrivateClass")

```

### Как это работает

1. Mockito перехватывает вызов конструктора через `Mockito.mockConstruction()`
2. Создаётся стаб/spy вместо реального экземпляра
3. `StubConstructionAnswer` сохраняет связь стаб → оригинальный объект для spy-режима
4. Вызовы методов на стабе обрабатываются через `StubAnswer`

StubStaticMethods
--------------

### Описание

Стабит статические методы указанного класса.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(StubStaticMethods.List.class)
public @interface StubStaticMethods {
    String name() default "";                    // Логическое имя конфигурации  
    Class<?> stubClass() default Null.class;     // Класс для стабирования статики
    String stubClassName() default "";           // FQN класса (альтернатива stubClass)
    String[] methods() default {};               // Конкретные статические методы  
    boolean spy() default false;                 // Spy вместо полного стаба
    boolean cloneArgsAndResult() default false;  // Клонирование аргументов/результатов  
}  

```

### Когда использовать

**Используйте `@StubStaticMethods` для стабирования статических методов utility-классов:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubStaticMethods(stubClass = IdGenerator.class, methods = {"generate"})
public class OrderTest {  
    // IdGenerator.generate() будет перехвачен  
}  

```

**Типичные сценарии использования:**

* Utility-классы с статическими методами (IdGenerator, CryptoUtils, Validators)
* Статические фабрики
* Классы-помощники для работы с файлами, сетью, БД
* Генераторы случайных значений (кроме времени)

### Примеры использования

**Стабирование статического метода utility-класса:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubStaticMethods(stubClass = IdGenerator.class, methods = {"generate"})
public class OrderTest {  
    // IdGenerator.generate() вернёт значение из <stubCall>
}  

```

**Mock для крипто-утилит:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubStaticMethods(stubClassName = "com.example.utils.CryptoHelper")
public class CryptoTest {  
    // Все статические методы CryptoHelper будут заменены стабами
}  

```

**Несколько статических стабов:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@StubStaticMethods(stubClass = IdGenerator.class, methods = {"generate"})
@StubStaticMethods(stubClass = Validators.class, methods = {"isValid"})
public class ValidationTest {  
    // Оба статических класса будут заменены стабами
}  

```

### Когда использовать stubClassName

`stubClassName` полезен когда класс **недоступен напрямую** :

* Private классы
* Внутренние классы
* Классы без прямого доступа в classpath теста

```
java  
@StubStaticMethods(stubClassName = "com.example.internal.InternalUtils")

```

### Особенности

* Использует `Mockito.mockStatic()` для перехвата
* Требует ByteBuddy для инструментации классов
* Статические стабы активны только в пределах теста

⚠️ **Важно:** Для подмены времени используйте `@MockJavaTime` и `NowSetter`, а не `<stubCall>`
для `System.currentTimeMillis()`.

Интеграция с XML-тестами
------------------------

Стабы настраиваются через элемент `<stubCall>` в тестовом файле:

```
xml  
<test type="Case" name="со стабом">
    <bean>orderService</bean>  
    <method>create</method>  
    <request>  
        <value>ORDER-123</value>  
    </request>  
    
    <!-- Ожидаемый вызов стаба -->
    <stubCall name="paymentService" method="charge">
        <args>  
            <value>ORDER-123</value>  
            <value type="BigDecimal">100.00</value>  
        </args>  
        <!-- Ожидаемый результат -->  
        <result>  
            <return type="PaymentResult">  
                <status>SUCCESS</status>  
            </return>  
        </result>  
    </stubCall>
    
    <response type="Order">  
        <id>ORDER-123</id>  
        <status>CREATED</status>  
    </response>  
</test>  

```

### Наследование stubCall

Элементы `<stubCall>` **наследуются вверх по иерархии тестов** . Это позволяет вынести часто используемые стабы на
уровень контейнера или даже корня:

```
xml  
<test type="Container">  
    <!-- Общие стабы для всех тестов в этом контейнере -->
    <stubCall name="IdGenerator" method="generate">
        <result>  
            <return type="String">ID-001</return>  
        </result>  
        <result>  
            <return type="String">ID-002</return>  
        </result>  
        <result>  
            <return type="String">ID-003</return>  
        </result>  
    </stubCall>
    
    <test type="Case" name="тест1">  
        <bean>service1</bean>  
        <method>run</method>  
        <!-- IdGenerator.generate() будет возвращать значения из parent stubCall -->
    </test>  
    
    <test type="Case" name="тест2">  
        <bean>service2</bean>  
        <method>run</method>  
        <!-- IdGenerator.generate() будет возвращать значения из parent stubCall -->
    </test>  
</test>  

```

**Поиск stubCall** происходит от текущего теста вверх к корню --- используется первое совпадение
по `name`/`method`/`args`.

### Удаление неиспользованных stubCall

При обновлении тестового файла неиспользованные объявления `<stubCall>` удаляются из leaf-тестов: `Part` или `Case`
без вложенных частей. Контейнеры и `Case` с частями сохраняют свои `<stubCall>`, потому что их могут использовать
вложенные тесты.

Вызов считается использованным, если он был сопоставлен во время выполнения теста (включая вызовы с
`disabled="true"`) или был записан после реального вызова метода.

### Атрибуты stubCall

| Атрибут    | Описание                                                   |
|------------|------------------------------------------------------------|
| `name`     | Имя стаба (совпадает с `name` в аннотации или именем бина) |
| `method`   | Имя метода для перехвата                                   |
| `disabled` | Пропустить проверку этого вызова                           |

### Элементы внутри stubCall

| Элемент    | Описание                                        |
|------------|-------------------------------------------------|
| `<args>`   | Ожидаемые аргументы вызова (список `<value>`)   |
| `<result>` | Ожидаемый результат: `<return>` или `<throw>`   |
| `<return>` | Значение для возврата из стабированного метода  |
| `<throw>`  | Исключение для выброса из стабированного метода |

### Последовательность результатов

Если указано несколько `<result>`, они возвращаются по очереди:

```
xml  
<stubCall name="counter" method="next">
    <result><return type="Integer">1</return></result>  
    <result><return type="Integer">2</return></result>  
    <result><return type="Integer">3</return></result>  
</stubCall>

```

Первый вызов `counter.next()` вернёт `1`, второй --- `2`, третий --- `3`, затем цикл повторяется.

### Кастомное сравнение аргументов

Для сложных типов зарегистрируйте кастомное сравнение в `TestSetupModule`:

```
java  
@Configuration  
public class TestConfig {  
    @Bean  
    TestSetupModule testSetupModule() {  
        return new TestSetupModule()  
            .addEqualsForType(MyComplexType.class, (a, b) -> a.getId().equals(b.getId()));  
    }  
}  

```

Механизм работы StubAnswer
--------------------------

### Жизненный цикл перехвата

```
Вызов метода на стабе/spy
    ↓  
StubAnswer.answer()
    ├── enabled = false? → callRealMethod()  
    ├── methods != null && метод не в списке? → callRealMethod()  
    ├── stubCall.disabled = true? → callRealMethod()
    │  
    ├── Поиск StubCall по name/method/args (вверх по иерархии)
    │   ├── Не найден + callRealMethodsOnNoData = true → callRealMethod()
    │   ├── Не найден + returnMockOnNoData = true → вернуть deep mock
    │   └── Не найден + оба false → ошибка (нет ожидаемого вызова)  
    │  
    ├── Найден StubCall
    │   ├── Запись вызова в test.stubCall (если addStubCall = true)
    │   ├── callRealMethod = true? → выполнить реальный метод + записать результат  
    │   ├── tryThrowException() → выбросить исключение, если задано  
    │   └── Вернуть результат (клонированный, если cloneArgsAndResult = true)  

```

### Глобальный флаг enabled

Перехват активен **только** внутри `StubAnswer.enable()`:

```
java  
// В TestExecutor.runTest()  
StubAnswer.enable(() -> {
    // Здесь перехват активен  
    test.executeMethod(...);  // Вызовы на стабе будут перехвачены
});  
// Здесь перехват отключён  

```

Это предотвращает перехват вызовов вне контекста теста.

Частые проблемы
---------------

### Стаб не применяется

**Причина:** Несоответствие имени или типа в аннотации и конфигурации.  
**Решение:**

```
java  
// ✅ Правильно: имя совпадает с @Bean или @Component  
@StubBean(name = "myService")

// ❌ Ошибка: имя не совпадает  
@StubBean(name = "wrongName")

```

### Конфликт имён стабов

**Причина:** Несколько стабов с одинаковым `name`.
**Решение:**

```
java  
// ✅ Использовать уникальные имена  
@StubBean(name = "paymentServiceV1")
@StubBean(name = "paymentServiceV2")

```

### Аргументы не совпадают при сравнении

**Причина:** Сравнение аргументов в `StubCall` использует рекурсивное сравнение AssertJ.
**Решение:**

* Убедитесь, что типы аргументов совпадают
* Для сложных объектов зарегистрируйте кастомное сравнение в `TestSetupModule`:

```
java  
@Bean  
TestSetupModule testSetupModule() {  
    return new TestSetupModule()  
        .addEqualsForType(MyComplexType.class, (a, b) -> a.getId().equals(b.getId()));  
}  

```

### Статический стаб не работает

**Причина:** Класс не может быть переопределён (final, system class).  
**Решение:**

* Убедитесь, что класс не `final`
* Для system-классов может потребоваться дополнительная конфигурация ByteBuddy

Рекомендации
------------

1. **Используйте `name` для явной идентификации** --- особенно когда стабится несколько бинов одного типа.
2. **Используйте spy для выборочного выполнения реальных методов** --- например, для выброса исключений в определённых
   условиях.
3. **Включайте `cloneArgsAndResult` для мутабельных объектов** --- предотвращает неожиданные изменения данных между
   тестом и стабом.
4. **Выносите общие стабы на уровень контейнера** --- часто используемые стабы (IdGenerator, случайные значения) можно
   определить в родительском `Container` для переиспользования.
5. **Не используйте `<stubCall>` для времени** --- для управления временем используйте `@MockJavaTime` и `NowSetter`.
6. **Проверяйте уникальность `name`** --- дублирование имён приводит к `IllegalArgumentException`.
7. **Используйте `disabled="true"` в `<stubCall>`** для временного отключения проверки вызова без удаления из XML.
8. **Регистрируйте кастомное сравнение в `TestSetupModule`** --- используйте `addEqualsForType()` для сложных типов
   аргументов.
9. **Используйте `stubClassName` для недоступных классов** --- private, внутренние классы, классы без прямого доступа.
