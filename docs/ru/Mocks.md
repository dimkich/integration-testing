Моки в интеграционных тестах (TestBeanMock, TestConstructorMock, TestStaticMock)
================================================================================

Обзор
-----

Фреймворк предоставляет три аннотации для настройки моков на основе Mockito:

| Аннотация              | Назначение                      | Область применения                                     |
|------------------------|---------------------------------|--------------------------------------------------------|
| `@TestBeanMock`        | Мокирование Spring-бинов        | Экземпляры бинов в контексте                           |
| `@TestConstructorMock` | Мокирование конструкторов       | Любые объекты, создаваемые через конструктор (не бины) |
| `@TestStaticMock`      | Мокирование статических методов | Статические методы классов                             |

Все аннотации:

* Применяются на уровне тестового класса
* Поддерживают повторение (repeatable)
* Интегрируются с системой `MockInvoke` для конфигурации ожидаемых вызовов в XML

TestBeanMock
------------

### Описание

Мокирует или создаёт spy для **Spring-бина** по имени или типу.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(TestBeanMock.List.class)  
public @interface TestBeanMock {  
    String name() default "";                    // Имя бина в Spring-контексте  
    Class<?> mockClass() default Null.class;     // Тип бина (если name не указан)  
    String mockClassName() default "";           // FQN типа (альтернатива mockClass)  
    String[] methods() default {};               // Конкретные методы для мокирования  
    boolean spy() default false;                 // Использовать spy вместо mock  
    boolean cloneArgsAndResult() default false;  // Глубокое клонирование аргументов/результатов  
}  

```

### Когда использовать

**Используйте `@TestBeanMock` когда нужно мокать Spring-бин:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestBeanMock(name = "paymentService")  
public class OrderTest {  
    // paymentService будет заменён на mock  
}  

```

### Примеры использования

**Мокирование бина по имени:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestBeanMock(name = "paymentService")  
public class OrderTest {  
    // paymentService будет заменён на mock  
}  

```

**Несколько моков:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestBeanMock(name = "emailService")  
@TestBeanMock(name = "smsService", spy = true)  
public class NotificationTest {  
    // emailService --- mock, smsService --- spy  
}  

```

**Spy для бина по типу:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestBeanMock(mockClass = UserService.class, spy = true)  
public class UserTest {  
    // Все бины типа UserService будут spy  
}  

```

### Когда использовать spy

`spy = true` используется когда нужно **иногда выполнять реальный метод, а иногда нет** :

```
java  
@TestBeanMock(name = "paymentService", spy = true)  

```

**Типичные сценарии:**

* Выброс исключений в определённых условиях
* Частичное сохранение реальной логики
* Верификация вызовов с сохранением поведения

**Важно:** В моках тоже можно задать конкретные методы через `methods = {...}`. Spy не нужен только для ограничения
мокируемых методов.

### Поведение

| Параметр                    | Значение по умолчанию              | Эффект                                                 |
|-----------------------------|------------------------------------|--------------------------------------------------------|
| `spy = false`               | Полное мокирование                 | Методы не выполняются, если не заданы в `<mockInvoke>` |
| `spy = true`                | Spy-режим                          | Реальные методы выполняются, если не переопределены    |
| `cloneArgsAndResult = true` | Аргументы и результаты клонируются | Избегает побочных эффектов от общих ссылок             |

TestConstructorMock
-------------------

### Описание

Мокирует вызовы конструкторов указанного класса.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(TestConstructorMock.List.class)  
public @interface TestConstructorMock {  
    String name() default "";                    // Логическое имя конфигурации  
    Class<?> mockClass() default Null.class;     // Класс для мокирования конструкторов  
    String mockClassName() default "";           // FQN класса (альтернатива mockClass)  
    String[] methods() default {};               // Сигнатуры конструкторов (опционально)  
    boolean spy() default false;                 // Spy для созданных экземпляров  
    boolean cloneArgsAndResult() default false;  // Клонирование аргументов/результатов  
}  

```

### Когда использовать

**Используйте `@TestConstructorMock` когда нужно мокать объекты, которые не являются Spring-бинами:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestConstructorMock(mockClass = ExternalApiClient.class)  
public class ApiTest {  
    // new ExternalApiClient(...) вернёт mock вместо реального объекта  
}  

```

**Подходит для любого объекта, который создаётся через конструктор:**

* DTO и модели данных
* Клиенты внешних сервисов
* Утилитные классы
* Любые POJO, создаваемые через `new`

### Примеры использования

**Мокирование всех конструкторов класса:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestConstructorMock(mockClass = ExternalApiClient.class)  
public class ApiTest {  
    // new ExternalApiClient(...) вернёт mock вместо реального объекта  
}  

```

**Spy для созданных экземпляров:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestConstructorMock(mockClass = Order.class, spy = true)  
public class OrderTest {  
    // new Order(...) создаст реальный объект, но обернёт в spy  
}  

```

### Когда использовать mockClassName

`mockClassName` полезен когда класс **недоступен напрямую** :

* Private классы
* Внутренние классы
* Классы без прямого доступа в classpath теста

```
java  
@TestConstructorMock(mockClassName = "com.example.internal.PrivateClass")  

```

### Как это работает

1. Mockito перехватывает вызов конструктора через `Mockito.mockConstruction()`
2. Создаётся mock/spy вместо реального экземпляра
3. `ConstructorMockAnswer` сохраняет связь mock → оригинальный объект для spy-режима
4. Вызовы методов на mock обрабатываются через `MockAnswer`

TestStaticMock
--------------

### Описание

Мокирует статические методы указанного класса.

```
java  
@Inherited  
@Target(TYPE)  
@Retention(RUNTIME)  
@Repeatable(TestStaticMock.List.class)  
public @interface TestStaticMock {  
    String name() default "";                    // Логическое имя конфигурации  
    Class<?> mockClass() default Null.class;     // Класс для мокирования статики  
    String mockClassName() default "";           // FQN класса (альтернатива mockClass)  
    String[] methods() default {};               // Конкретные статические методы  
    boolean spy() default false;                 // Spy вместо полного мока  
    boolean cloneArgsAndResult() default false;  // Клонирование аргументов/результатов  
}  

```

### Когда использовать

**Используйте `@TestStaticMock` для мокирования статических методов utility-классов:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestStaticMock(mockClass = IdGenerator.class, methods = {"generate"})  
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

**Мокирование статического метода utility-класса:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestStaticMock(mockClass = IdGenerator.class, methods = {"generate"})  
public class OrderTest {  
    // IdGenerator.generate() вернёт значение из <mockInvoke>  
}  

```

**Mock для крипто-утилит:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestStaticMock(mockClassName = "com.example.utils.CryptoHelper")  
public class CryptoTest {  
    // Все статические методы CryptoHelper будут мокированы  
}  

```

**Несколько статических моков:**

```
java  
@IntegrationTesting  
@SpringBootTest  
@TestStaticMock(mockClass = IdGenerator.class, methods = {"generate"})  
@TestStaticMock(mockClass = Validators.class, methods = {"isValid"})  
public class ValidationTest {  
    // Оба статических класса будут мокированы  
}  

```

### Когда использовать mockClassName

`mockClassName` полезен когда класс **недоступен напрямую** :

* Private классы
* Внутренние классы
* Классы без прямого доступа в classpath теста

```
java  
@TestStaticMock(mockClassName = "com.example.internal.InternalUtils")  

```

### Особенности

* Использует `Mockito.mockStatic()` для перехвата
* Требует ByteBuddy для инструментации классов
* Статические моки активны только в пределах теста

⚠️ **Важно:** Для мокирования времени используйте `@MockJavaTime` и `NowSetter`, а не `<mockInvoke>`
для `System.currentTimeMillis()`.

Интеграция с XML-тестами
------------------------

Моки настраиваются через элемент `<mockInvoke>` в тестовом файле:

```
xml  
<test type="Case" name="сМоком">  
    <bean>orderService</bean>  
    <method>create</method>  
    <request>  
        <value>ORDER-123</value>  
    </request>  
    
    <!-- Ожидаемый вызов мока -->  
    <mockInvoke name="paymentService" method="charge">  
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
    </mockInvoke>  
    
    <response type="Order">  
        <id>ORDER-123</id>  
        <status>CREATED</status>  
    </response>  
</test>  

```

### Наследование mockInvoke

Элементы `<mockInvoke>` **наследуются вверх по иерархии тестов** . Это позволяет вынести часто используемые моки на
уровень контейнера или даже корня:

```
xml  
<test type="Container">  
    <!-- Общие моки для всех тестов в этом контейнере -->  
    <mockInvoke name="IdGenerator" method="generate">  
        <result>  
            <return type="String">ID-001</return>  
        </result>  
        <result>  
            <return type="String">ID-002</return>  
        </result>  
        <result>  
            <return type="String">ID-003</return>  
        </result>  
    </mockInvoke>  
    
    <test type="Case" name="тест1">  
        <bean>service1</bean>  
        <method>run</method>  
        <!-- IdGenerator.generate() будет возвращать значения из parent mockInvoke -->  
    </test>  
    
    <test type="Case" name="тест2">  
        <bean>service2</bean>  
        <method>run</method>  
        <!-- IdGenerator.generate() будет возвращать значения из parent mockInvoke -->  
    </test>  
</test>  

```

**Поиск mockInvoke** происходит от текущего теста вверх к корню --- используется первое совпадение
по `name`/`method`/`args`.

### Атрибуты mockInvoke

| Атрибут    | Описание                                                  |
|------------|-----------------------------------------------------------|
| `name`     | Имя мока (совпадает с `name` в аннотации или именем бина) |
| `method`   | Имя метода для перехвата                                  |
| `disabled` | Пропустить проверку этого вызова                          |

### Элементы внутри mockInvoke

| Элемент    | Описание                                       |
|------------|------------------------------------------------|
| `<args>`   | Ожидаемые аргументы вызова (список `<value>`)  |
| `<result>` | Ожидаемый результат: `<return>` или `<throw>`  |
| `<return>` | Значение для возврата из мокированного метода  |
| `<throw>`  | Исключение для выброса из мокированного метода |

### Последовательность результатов

Если указано несколько `<result>`, они возвращаются по очереди:

```
xml  
<mockInvoke name="counter" method="next">  
    <result><return type="Integer">1</return></result>  
    <result><return type="Integer">2</return></result>  
    <result><return type="Integer">3</return></result>  
</mockInvoke>  

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

Механизм работы MockAnswer
--------------------------

### Жизненный цикл перехвата

```
Вызов метода на mock/spy  
    ↓  
MockAnswer.answer()  
    ├── enabled = false? → callRealMethod()  
    ├── methods != null && метод не в списке? → callRealMethod()  
    ├── mockInvoke.disabled = true? → callRealMethod()  
    │  
    ├── Поиск MockInvoke по name/method/args (вверх по иерархии)  
    │   ├── Не найден + mockCallRealMethodsOnNoData = true → callRealMethod()  
    │   ├── Не найден + mockReturnMockOnNoData = true → вернуть deep mock  
    │   └── Не найден + оба false → ошибка (нет ожидаемого вызова)  
    │  
    ├── Найден MockInvoke  
    │   ├── Запись вызова в test.mockInvoke (если addMockInvoke = true)  
    │   ├── callRealMethod = true? → выполнить реальный метод + записать результат  
    │   ├── tryThrowException() → выбросить исключение, если задано  
    │   └── Вернуть результат (клонированный, если cloneArgsAndResult = true)  

```

### Глобальный флаг enabled

Перехват активен **только** внутри `MockAnswer.enable()`:

```
java  
// В TestExecutor.runTest()  
MockAnswer.enable(() -> {  
    // Здесь перехват активен  
    test.executeMethod(...);  // Вызовы на mock будут перехвачены  
});  
// Здесь перехват отключён  

```

Это предотвращает перехват вызовов вне контекста теста.

Частые проблемы
---------------

### Mock не применяется

**Причина:** Несоответствие имени или типа в аннотации и конфигурации.  
**Решение:**

```
java  
// ✅ Правильно: имя совпадает с @Bean или @Component  
@TestBeanMock(name = "myService")  

// ❌ Ошибка: имя не совпадает  
@TestBeanMock(name = "wrongName")  

```

### Конфликт имён моков

**Причина:** Несколько моков с одинаковым `name`.  
**Решение:**

```
java  
// ✅ Использовать уникальные имена  
@TestBeanMock(name = "paymentServiceV1")  
@TestBeanMock(name = "paymentServiceV2")  

```

### Аргументы не совпадают при сравнении

**Причина:** Сравнение аргументов в `MockInvoke` использует рекурсивное сравнение AssertJ.  
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

### Static mock не работает

**Причина:** Класс не может быть переопределён (final, system class).  
**Решение:**

* Убедитесь, что класс не `final`
* Для system-классов может потребоваться дополнительная конфигурация ByteBuddy

Рекомендации
------------

1. **Используйте `name` для явной идентификации** --- особенно когда мокируется несколько бинов одного типа.
2. **Используйте spy для выборочного выполнения реальных методов** --- например, для выброса исключений в определённых
   условиях.
3. **Включайте `cloneArgsAndResult` для мутабельных объектов** --- предотвращает неожиданные изменения данных между
   тестом и моком.
4. **Выносите общие моки на уровень контейнера** --- часто используемые моки (IdGenerator, случайные значения) можно
   определить в родительском `Container` для переиспользования.
5. **Не используйте `<mockInvoke>` для времени** --- для управления временем используйте `@MockJavaTime` и `NowSetter`.
6. **Проверяйте уникальность `name`** --- дублирование имён приводит к `IllegalArgumentException`.
7. **Используйте `disabled="true"` в `<mockInvoke>`** для временного отключения проверки вызова без удаления из XML.
8. **Регистрируйте кастомное сравнение в `TestSetupModule`** --- используйте `addEqualsForType()` для сложных типов
   аргументов.
9. **Используйте `mockClassName` для недоступных классов** --- private, внутренние классы, классы без прямого доступа.  
