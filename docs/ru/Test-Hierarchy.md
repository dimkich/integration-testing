Иерархия тестов
===============

Структура тестов
----------------

Фреймворк использует трёхуровневую иерархию тестов для организации тестовых сценариев:

```
test type="Container" (корень, обязателен, name не требуется)  
├── test type="Container" (вложенный контейнер, name обязателен)  
│   ├── test type="Container" (можно вкладывать глубже, name обязателен)  
│   └── test type="Case" (тест-кейс, name обязателен)  
│       └── test type="Part" (часть кейса, name обязателен)  
└── test type="Case" (тест-кейс, name обязателен)  
    └── test type="Part" (часть кейса, name обязателен)  

```

Типы тестов
-----------

| Тип         | Класс           | Описание                   | Может быть родителем | Может быть ребёнком           | `name`                                             |
|-------------|-----------------|----------------------------|----------------------|-------------------------------|----------------------------------------------------|
| `Container` | `TestContainer` | Группирует тесты логически | `Container`, `Case`  | Только корень или `Container` | Необязательно для корня, обязательно для вложенных |
| `Case`      | `TestCase`      | Отдельный тест-сценарий    | `Part`               | Только `Container`            | **Обязательно**                                    |
| `Part`      | `TestPart`      | Шаг многошагового теста    | Нет                  | Только `Case`                 | **Обязательно**                                    |

Правила иерархии
----------------

### Обязательные правила

1. **Корень всегда `Container`**

   ```
   xml  
   <!-- ✅ Правильно: корень без name -->  
   <test type="Container">  
       <test type="Case" name="test1">...</test>  
   </test>  

   <!-- ❌ Ошибка: корень должен быть Container -->  
   <test type="Case" name="root">  
       <bean>service</bean>  
   </test>  

   ```

2. **`name` обязателен для всех кроме корня**

   ```
   xml  
   <!-- ✅ Правильно -->  
   <test type="Container">  
       <test type="Case" name="сценарий1">...</test>  
       <test type="Container" name="группа">  
           <test type="Case" name="сценарий2">...</test>  
       </test>  
   </test>  

   <!-- ❌ Ошибка: Case без name -->  
   <test type="Container">  
       <test type="Case">...</test>  
   </test>  

   ```

3. **`Case` только внутри `Container`**

   ```
   xml  
   <!-- ✅ Правильно -->  
   <test type="Container">  
       <test type="Case" name="test1">...</test>  
   </test>  

   <!-- ❌ Ошибка: Case не может быть ребёнком Case -->  
   <test type="Case" name="parent">  
       <test type="Case" name="child">...</test>  
   </test>  

   ```

4. **`Part` только внутри `Case`**

   ```
   xml  
   <!-- ✅ Правильно -->  
   <test type="Case" name="multiStep">  
       <test type="Part" name="step1">...</test>  
       <test type="Part" name="step2">...</test>  
   </test>  

   <!-- ❌ Ошибка: Part не может быть ребёнком Container -->  
   <test type="Container" name="root">  
       <test type="Part" name="step1">...</test>  
   </test>  

   ```

Изоляция и наследование
-----------------------

### Модель изоляции

| Уровень     | Изоляция контекста | Наследование настроек            | Сброс состояния              |
|-------------|--------------------|----------------------------------|------------------------------|
| `Container` | Да                 | От родительских контейнеров      | Нет                          |
| `Case`      | **Да**             | От всех родительских `Container` | **Да** (перед каждым кейсом) |
| `Part`      | **Нет**            | От родительского `Case`          | **Нет** (делят состояние)    |

### Как работает наследование

Настройки наследуются **сверху вниз** по иерархии:

```
xml  
<test type="Container" name="модуль">  
    <!-- Настройки этого контейнера применятся ко всем кейсам внутри -->  
    <init>  
        <!-- Конкретные действия инициализации описаны в отдельном разделе -->  
    </init>  
    
    <test type="Case" name="кейс1">  
        <!-- Перед выполнением: настройки из parent применены, состояние сброшено -->  
        <bean>service</bean>  
        <method>test1</method>  
    </test>  
    
    <test type="Case" name="кейс2">  
        <!-- Перед выполнением: настройки из parent применены, состояние сброшено -->  
        <bean>service</bean>  
        <method>test2</method>  
    </test>  
</test>  

```

### Поведение `TestCase`

Каждый `TestCase`:

1. **Наследует настройки** от всех родительских `Container` (цепочка до корня)
2. **Получает изолированное окружение** --- состояние сбрасывается перед выполнением
3. **Выполняется независимо** от других `Case` --- **не видит** результаты предыдущих кейсов

```
xml  
<test type="Container" name="группа">  
    <init>  
        <!-- Настройки инициализации, применяемые ко всем кейсам -->  
    </init>  
    
    <test type="Case" name="созданиеЗаказа">  
        <!-- Перед запуском: настройки применены, состояние сброшено -->  
        <bean>orderService</bean>  
        <method>create</method>  
    </test>  
    
    <test type="Case" name="оплатаЗаказа">  
        <!-- Перед запуском: настройки применены, состояние сброшено -->  
        <!-- НЕ видит результаты предыдущего кейса -->  
        <bean>paymentService</bean>  
        <method>pay</method>  
    </test>  
</test>  

```

### Поведение `TestPart`

`TestPart` внутри одного `TestCase`:

1. **Наследуют настройки** от родительского `Case` и всех `Container`
2. **Не получают сброс состояния** --- делят окружение с другими `Part`
3. **Выполняются последовательно** --- каждый `Part` видит изменения от предыдущих

```
xml  
<test type="Case" name="многошаговыйСценарий">  
    <!-- Настройки кейса применяются один раз перед всеми Part -->  
    <init>  
        <!-- Настройки инициализации -->  
    </init>  
    
    <test type="Part" name="шаг1">  
        <!-- Таблица очищена перед шагом 1 (из init) -->  
        <bean>orderService</bean>  
        <method>create</method>  
        <!-- После выполнения: в таблице есть новый заказ -->  
    </test>  
    
    <test type="Part" name="шаг2">  
        <!-- Таблица НЕ очищается между шагами -->  
        <!-- Видит заказ, созданный в шаге 1 -->  
        <bean>paymentService</bean>  
        <method>pay</method>  
    </test>  
    
    <test type="Part" name="шаг3">  
        <!-- Таблица НЕ очищается между шагами -->  
        <!-- Видит заказ и оплату из предыдущих шагов -->  
        <bean>deliveryService</bean>  
        <method>ship</method>  
    </test>  
</test>  

```

### Сравнение `Case` vs `Part`

| Характеристика                     | `TestCase`                | `TestPart`                                    |
|------------------------------------|---------------------------|-----------------------------------------------|
| Изоляция контекста                 | Да                        | Нет                                           |
| Наследование настроек `init`       | От всех родителей         | От всех родителей                             |
| Сброс состояния перед выполнением  | **Да**                    | **Нет**                                       |
| Видит результаты предыдущих тестов | **Нет** (полная изоляция) | **Да** (от предыдущих `Part` в том же `Case`) |
| Когда использовать                 | Независимые сценарии      | Многошаговые сценарии с зависимостями         |

### Пример: когда использовать `Case`, когда `Part`

**Используйте отдельные `Case`** для независимых сценариев:

```
xml  
<test type="Container" name="orderTests">  
    <init>  
        <!-- Настройки, применяемые ко всем кейсам -->  
    </init>  
    
    <!-- Каждый кейс начинает с чистого состояния -->  
    <test type="Case" name="созданиеЗаказа">  
        <bean>orderService</bean>  
        <method>create</method>  
    </test>  
    
    <test type="Case" name="удалениеЗаказа">  
        <!-- Состояние сброшено перед этим кейсом -->  
        <bean>orderService</bean>  
        <method>delete</method>  
    </test>  
    
    <test type="Case" name="поискЗаказа">  
        <!-- Состояние сброшено перед этим кейсом -->  
        <bean>orderService</bean>  
        <method>find</method>  
    </test>  
</test>  

```

**Используйте `Part`** для зависимых шагов одного сценария:

```
xml  
<test type="Case" name="полныйЦиклЗаказа">  
    <init>  
        <!-- Настройки применяются один раз перед всеми Part -->  
    </init>  
    
    <!-- Шаг 1: создаёт заказ -->  
    <test type="Part" name="создание">  
        <bean>orderService</bean>  
        <method>create</method>  
    </test>  
    
    <!-- Шаг 2: использует результат шага 1 -->  
    <test type="Part" name="оплата">  
        <bean>paymentService</bean>  
        <method>pay</method>  
    </test>  
    
    <!-- Шаг 3: использует результаты шагов 1-2 -->  
    <test type="Part" name="доставка">  
        <bean>deliveryService</bean>  
        <method>ship</method>  
    </test>  
</test>  

```

Атрибуты теста
--------------

| Атрибут    | Тип     | Обязательный         | Описание                                                             |
|------------|---------|----------------------|----------------------------------------------------------------------|
| `type`     | String  | **Да**               | Тип теста: `Container`, `Case`, `Part`                               |
| `name`     | String  | **Да** (кроме корня) | Имя теста (отображается в отчёте JUnit, используется для фильтрации) |
| `disabled` | Boolean | Нет                  | Отключить тест (`true`/`false`, по умолчанию `false`)                |

### Наследование `disabled`

Если родитель отключен, все дочерние тесты также пропускаются:

```
xml  
<test type="Container" name="группа" disabled="true">  
    <!-- Все тесты внутри будут пропущены -->  
    <test type="Case" name="тест1">...</test>  
    <test type="Case" name="тест2">...</test>  
</test>  

```

Поля теста
----------

| Поле              | Тип              | Описание                                |
|-------------------|------------------|-----------------------------------------|
| `bean`            | String           | Имя Spring-бина для вызова метода       |
| `method`          | String           | Имя метода для вызова                   |
| `request`         | List<Object>     | Аргументы метода (список `<value>`)     |
| `response`        | Object           | Ожидаемый результат выполнения          |
| `mockInvoke`      | List             | Ожидаемые вызовы моков                  |
| `inboundMessage`  | MessageDto       | Входное сообщение для тестирования      |
| `outboundMessage` | List<MessageDto> | Ожидаемые исходящие сообщения           |
| `custom`          | Map              | Пользовательские данные для теста       |
| `init`            | List<TestInit>   | Конфигурация инициализации перед тестом |

💡 **Примечание:** Детальное описание поля `init` и доступных действий инициализации представлено в отдельном разделе
документации.

Примеры
-------

### Минимальный тест

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<test type="Container">  
    <test type="Case" name="минимальный">  
        <bean>service</bean>  
        <method>run</method>  
        <response>ok</response>  
    </test>  
</test>  

```

### Вложенные контейнеры

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<test type="Container">  
    <test type="Container" name="модульА">  
        <test type="Case" name="сценарий1">  
            <bean>serviceA</bean>  
            <method>method1</method>  
        </test>  
        <test type="Case" name="сценарий2">  
            <bean>serviceA</bean>  
            <method>method2</method>  
        </test>  
    </test>  
    <test type="Container" name="модульБ">  
        <test type="Case" name="сценарий1">  
            <bean>serviceB</bean>  
            <method>method1</method>  
        </test>  
    </test>  
</test>  

```

### Смешанная структура

```
xml  
<?xml version='1.0' encoding='UTF-8'?>  
<test type="Container">  
    <!-- Простой тест-кейс -->  
    <test type="Case" name="простойТест">  
        <bean>service</bean>  
        <method>simple</method>  
        <response>result</response>  
    </test>  
    
    <!-- Контейнер с группой тестов -->  
    <test type="Container" name="группаСложных">  
        <test type="Case" name="многошаговый">  
            <test type="Part" name="шаг1">  
                <bean>service</bean>  
                <method>step1</method>  
            </test>  
            <test type="Part" name="шаг2">  
                <bean>service</bean>  
                <method>step2</method>  
            </test>  
        </test>  
        <test type="Case" name="ещёОдинПростой">  
            <bean>service</bean>  
            <method>another</method>  
        </test>  
    </test>  
</test>  

```

Фильтрация тестов
-----------------

Можно запускать только определённые тесты по пути имён от корня:

```
java  
@TestFactory  
Stream<DynamicNode> tests() throws Exception {  
    // Запустить только: Container → "модульА" → "сценарий1"  
    return dynamicTestBuilder.build("tests/all.xml", List.of("модульА", "сценарий1"));  
}  

```

### Как работает фильтр

Фильтр выбирает **путь от корня** по именам тестов:

```
xml  
<test type="Container">  
    <test type="Container" name="модульА">  
        <test type="Case" name="сценарий1">...</test>  
        <test type="Case" name="сценарий2">...</test>  
    </test>  
    <test type="Container" name="модульБ">  
        <test type="Case" name="сценарий1">...</test>  
    </test>  
</test>  

```

```
java  
// Запустит только "модульА" → "сценарий1"  
dynamicTestBuilder.build("tests/all.xml", List.of("модульА", "сценарий1"));  

// Запустит только "модульБ" → "сценарий1"  
dynamicTestBuilder.build("tests/all.xml", List.of("модульБ", "сценарий1"));  

// Запустит всё (пустой фильтр = все тесты)  
dynamicTestBuilder.build("tests/all.xml");  

```

### Полное имя теста

Фреймворк формирует полное имя теста из всех родителей:

```
xml  
<test type="Container" name="модульА">  
    <test type="Case" name="сценарий1">  
        <test type="Part" name="шаг1">...</test>  
    </test>  
</test>  

```

Полное имя: `"модульА", "сценарий1", "шаг1"`  
Это имя отображается в отчётах JUnit и используется для фильтрации.

Зачем такая иерархия?
---------------------

| Причина                    | Объяснение                                                                                         |
|----------------------------|----------------------------------------------------------------------------------------------------|
| **Логическая группировка** | `Container` позволяет объединять связанные сценарии (по модулю, функциональности, API)             |
| **Частичное выполнение**   | Можно запускать отдельные ветки через фильтр имён в `DynamicTestBuilder.build(path, allowedNames)` |
| **Изоляция `Case`**        | Каждый `Case` выполняется в изолированном контексте --- нет побочных эффектов между кейсами        |
| **Зависимые шаги**         | `Part` внутри `Case` делят состояние --- удобно для многошаговых сценариев                         |
| **Наследование настроек**  | `init` и другие настройки наследуются сверху вниз, уменьшая дублирование                           |
| **Читаемость**             | Иерархия отражает структуру бизнес-сценариев (группа → сценарий → шаг)                             |
| **Отчётность**             | JUnit отображает иерархию в отчётах, упрощая навигацию по результатам                              |

все хорошо. что еще стоит описать. тесты запускаются только через бин (о чем написано) но есть еще inboundMessage тоже
может запускать тест. конейнер не может запускать тест. парт обязан запускать тест. кейс обязан либо запускать тест либо
быть контейнером для партов  
Завершено размышление

Иерархия тестов (дополнение)
============================

Типы тестов и выполнение
------------------------

| Тип         | Может запускать тест         | Описание                                                  |
|-------------|------------------------------|-----------------------------------------------------------|
| `Container` | **Нет**                      | Только группирует тесты, не выполняет код                 |
| `Case`      | **Да** (или содержит `Part`) | Либо запускает тест, либо является контейнером для `Part` |
| `Part`      | **Да** (обязан)              | Всегда запускает тест, не может иметь детей               |

Способы запуска теста
---------------------

Тест может быть выполнен **одним из двух способов** :

### 1. Вызов метода бина (основной способ)

```
xml  
<test type="Case" name="вызовМетода">  
    <bean>greetingService</bean>  
    <method>greet</method>  
    <request>  
        <value>Мир</value>  
    </request>  
    <response>Привет, Мир!</response>  
</test>  

```

### 2. Отправка входящего сообщения (альтернативный способ)

```
xml  
<test type="Case" name="входящееСообщение">  
    <inboundMessage>  
        <payload type="String">входные данные</payload>  
        <headers>  
            <header name="Content-Type">application/json</header>  
        </headers>  
    </inboundMessage>  
    <outboundMessage>  
        <payload type="String">исходящие данные</payload>  
    </outboundMessage>  
    <response type="String">ok</response>  
</test>  

```

⚠️ **Важно:** `bean`/`method` и `inboundMessage` --- **взаимоисключающие** способы запуска. Если
указан `inboundMessage`, то `bean` и `method` игнорируются.

Правила выполнения
------------------

### Container

* **Не выполняет** код теста
* Только группирует дочерние тесты
* Может содержать настройки `init`, которые наследуются дочерними тестами
* Может быть отключен через `disabled="true"` (все дочерние тесты также пропускаются)

```
xml  
<!-- Container не выполняет код, только группирует -->  
<test type="Container" name="группаТестов">  
    <init>  
        <!-- Настройки инициализации для всех дочерних тестов -->  
    </init>  
    <test type="Case" name="тест1">  
        <!-- Этот тест выполнится -->  
    </test>  
    <test type="Case" name="тест2">  
        <!-- Этот тест выполнится -->  
    </test>  
</test>  

```

### Case

* **Либо** выполняет тест (через `bean`/`method` или `inboundMessage`)
* **Либо** содержит дочерние `Part` (тогда сам не выполняет код, только группирует шаги)

```
xml  
<!-- Case выполняет тест напрямую -->  
<test type="Case" name="простойКейс">  
    <bean>service</bean>  
    <method>run</method>  
    <response>ok</response>  
</test>  

<!-- Case содержит Part, сам не выполняет код -->  
<test type="Case" name="многошаговыйКейс">  
    <test type="Part" name="шаг1">  
        <bean>service</bean>  
        <method>step1</method>  
    </test>  
    <test type="Part" name="шаг2">  
        <bean>service</bean>  
        <method>step2</method>  
    </test>  
</test>  

```

### Part

* **Всегда** выполняет тест (обязан иметь `bean`/`method` или `inboundMessage`)
* **Не может** иметь дочерних тестов
* Выполняется в контексте родительского `Case` (без сброса состояния между `Part`)

```
xml  
<!-- Part всегда выполняет тест -->  
<test type="Part" name="шаг">  
    <bean>service</bean>  
    <method>step</method>  
    <response>ok</response>  
</test>  

<!-- ❌ Ошибка: Part не может иметь детей -->  
<test type="Part" name="неправильно">  
    <bean>service</bean>  
    <method>step</method>  
    <test type="Part" name="ребёнок">  
        <!-- Это вызовет ошибку валидации -->  
    </test>  
</test>  

```

Диаграмма выполнения
--------------------

```
TestContainer (root)  
│  
├── TestContainer (не выполняет, только группирует)  
│   ├── TestCase (выполняет тест ИЛИ содержит Part)  
│   │   ├── TestPart (выполняет тест, обязан)  
│   │   └── TestPart (выполняет тест, обязан)  
│   └── TestCase (выполняет тест)  
│  
└── TestCase (выполняет тест ИЛИ содержит Part)  
    └── TestPart (выполняет тест, обязан)  

```

Валидация структуры
-------------------

Фреймворк проверяет структуру тестов при запуске:

| Проверка                                                 | Ошибка при нарушении                                                |
|----------------------------------------------------------|---------------------------------------------------------------------|
| Корень должен быть `Container`                           | `Root <test> must be of type="container"`                           |
| `Case` только внутри `Container`                         | `<test type="case"> can only be a child of <test type="container">` |
| `Part` только внутри `Case`                              | `<test type="part"> can only be a child of <test type="case">`      |
| `Part` должен запускать тест                             | (проверяется логически при выполнении)                              |
| `Case` должен либо запускать тест, либо содержать `Part` | (проверяется логически при выполнении)                              |

Примеры корректных структур
---------------------------

### Простой тест (Case как лист)

```
xml  
<test type="Container">  
    <test type="Case" name="простой">  
        <bean>service</bean>  
        <method>run</method>  
        <response>ok</response>  
    </test>  
</test>  

```

### Многошаговый тест (Case с Part)

```
xml  
<test type="Container">  
    <test type="Case" name="многошаговый">  
        <test type="Part" name="шаг1">  
            <bean>service</bean>  
            <method>step1</method>  
        </test>  
        <test type="Part" name="шаг2">  
            <bean>service</bean>  
            <method>step2</method>  
        </test>  
    </test>  
</test>  

```

### Тест с входящим сообщением

```
xml  
<test type="Container">  
    <test type="Case" name="сообщение">  
        <inboundMessage>  
            <payload type="String">данные</payload>  
        </inboundMessage>  
        <outboundMessage>  
            <payload type="String">ответ</payload>  
        </outboundMessage>  
        <response type="String">ok</response>  
    </test>  
</test>  

```

### Вложенные контейнеры

```
xml  
<test type="Container">  
    <test type="Container" name="модуль1">  
        <test type="Case" name="тест1">  
            <bean>service</bean>  
            <method>run</method>  
        </test>  
    </test>  
    <test type="Container" name="модуль2">  
        <test type="Case" name="тест2">  
            <test type="Part" name="шаг1">  
                <bean>service</bean>  
                <method>step1</method>  
            </test>  
        </test>  
    </test>  
</test>  

```

Примеры некорректных структур
-----------------------------

### ❌ Container пытается выполнить тест

```
xml  
<test type="Container" name="неправильно">  
    <bean>service</bean>  
    <method>run</method>  
    <!-- Container не должен иметь bean/method -->  
</test>  

```

### ❌ Case без теста и без Part

```
xml  
<test type="Case" name="неправильно">  
    <!-- Нет bean/method, нет inboundMessage, нет Part -->  
    <!-- Кейс ничего не делает -->  
</test>  

```

### ❌ Part с детьми

```
xml  
<test type="Part" name="неправильно">  
    <bean>service</bean>  
    <method>step</method>  
    <test type="Part" name="ребёнок">  
        <!-- Part не может иметь детей -->  
    </test>  
</test>  

```

### ❌ Part внутри Container

```
xml  
<test type="Container" name="неправильно">  
    <test type="Part" name="шаг">  
        <!-- Part должен быть внутри Case -->  
    </test>  
</test>  

```

### ❌ Case внутри Case

```
xml  
<test type="Case" name="родитель">  
    <test type="Case" name="ребёнок">  
        <!-- Case должен быть внутри Container -->  
    </test>  
</test>  

```

Сводная таблица
---------------

| Характеристика                    | Container                    | Case                         | Part            |
|-----------------------------------|------------------------------|------------------------------|-----------------|
| Может быть корнем                 | **Да** (обязан)              | Нет                          | Нет             |
| Может иметь детей                 | **Да** (`Container`, `Case`) | **Да** (`Part`)              | **Нет**         |
| Выполняет тест                    | **Нет**                      | **Да** (или содержит `Part`) | **Да** (обязан) |
| Может иметь `bean`/`method`       | Нет                          | Да                           | Да              |
| Может иметь `inboundMessage`      | Нет                          | Да                           | Да              |
| Может иметь `init`                | Да                           | Да                           | Да              |
| Сброс состояния перед выполнением | Н/Д                          | **Да**                       | **Нет**         |
| `name` обязателен                 | Нет (для корня)              | **Да**                       | **Да**          |
