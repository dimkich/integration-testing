# Детерминированные коллекции — `@DeterministicCollections`

Аннотация для интеграционных тестов, которая делает порядок обхода коллекций предсказуемым.
Она подменяет order-sensitive фабрики JDK и конструкторы `HashMap`/`HashSet` на их ordered-аналоги
(`LinkedHashMap`/`LinkedHashSet`) прямо в байткоде классов приложения.

## Зачем это нужно

Внутри JDK `Map.of`, `Map.ofEntries`, `Map.copyOf`, `Set.of`, `Set.copyOf` и
`Collectors.toUnmodifiableMap/Set` используют `ImmutableCollections` с **random SALT на каждый
запуск JVM**. Поэтому порядок обхода меняется от прогона к прогону даже для одинаковых аргументов:

```java
Map.of("a", 1, "b", 2).keySet().forEach(System.out::println); // a, b или b, a
```

`List.of` и `Stream.toList` этим не страдают — они индексируемые, поэтому аннотация их не трогает.

`HashMap`/`HashSet` тоже дают непредсказуемый порядок для ключей с identity-hash, а порядок
`Collectors.toSet`/`toMap`/`groupingBy` зависит от внутренней таблицы.

Если XML-снапшоты или `dataStorageDiff` содержат такие коллекции, тесты начинают «плавать».

## Быстрый старт

```java
@DeterministicCollections("com.example.myapp")
@SpringBootTest
public class MyIntegrationTest {
    // ...
}
```

`value()` — обязательный список префиксов fully qualified имён классов/пакетов. Инструментируются
только классы, чьё имя начинается с одного из префиксов.

## Что заменяется

| JDK API                                        | Результат                        |
|------------------------------------------------|----------------------------------|
| `Map.of(...)`, `Map.ofEntries(...)`            | unmodifiable `LinkedHashMap`     |
| `Map.copyOf(map)`                              | unmodifiable упорядоченная копия |
| `Set.of(...)` (включая varargs)                | unmodifiable `LinkedHashSet`     |
| `Set.copyOf(collection)`                       | unmodifiable упорядоченная копия |
| `Collectors.toSet()`                           | `LinkedHashSet`                  |
| `Collectors.toUnmodifiableSet()`               | unmodifiable `LinkedHashSet`     |
| `Collectors.toMap(k, v[, merge])`              | `LinkedHashMap`                  |
| `Collectors.toUnmodifiableMap(...)`            | unmodifiable `LinkedHashMap`     |
| `Collectors.groupingBy(...)` (1 и 2 аргумента) | `LinkedHashMap`                  |
| `new HashMap(...)`                             | `new LinkedHashMap(...)`         |
| `new HashSet(...)`                             | `new LinkedHashSet(...)`         |

## Что сохраняется

Меняется **только порядок итерации**. Контракт JDK остаётся прежним:

- `Map.of`, `Set.of`, `copyOf`, `toUnmodifiable*` остаются **имутабельными**;
- `toSet`, `toMap`, `groupingBy`, `new HashMap`, `new HashSet` остаются **мутабельными**;
- `null` в `Map.of`/`Set.of`/`copyOf` по-прежнему приводит к `NullPointerException`;
- дубликаты ключей/элементов по-прежнему приводят к `IllegalArgumentException`;
- `Collectors.toMap` без merge-функции по-прежнему кидает `IllegalStateException` на дубликат.

Это важно: если тест разрешит `map.put(...)` там, где в проде `Map.of` кинет
`UnsupportedOperationException`, тест начнёт пропускать реальные ошибки.

## Отключение групп

По умолчанию включены все замены. Если нужно временно отключить группу (например, для диагностики
или из-за проблем с переписыванием конструкторов), используйте `disabled`:

```java
@DeterministicCollections(
        value = "com.example.myapp",
        disabled = DeterministicCollections.Feature.HASH_COLLECTIONS)
```

Доступные группы:

| Группа             | Что отключает                                                            |
|--------------------|--------------------------------------------------------------------------|
| `MAP_OF`           | `Map.of` и `Map.ofEntries`                                               |
| `COPY_OF`          | `Map.copyOf` и `Set.copyOf`                                              |
| `SET_OF`           | `Set.of`                                                                 |
| `COLLECTORS`       | `toSet`, `toUnmodifiableSet`, `toMap`, `toUnmodifiableMap`, `groupingBy` |
| `HASH_COLLECTIONS` | `new HashMap` и `new HashSet`                                            |

Отключение `MAP_OF`, `SET_OF`, `COPY_OF` или `COLLECTORS` возвращает недетерминированный порядок —
используйте это только для диагностики. Основной практический сценарий — отключить
`HASH_COLLECTIONS`, если переписывание конструкторов конфликтует с конкретной библиотекой.

## Загрузка классов и повторная инструментация

Агент устанавливается до создания Spring-контекста, поэтому все бины и классы приложения
загружаются уже инструментированными. Уже загруженные классы (включая сам тестовый класс)
ретрансформируются при установке агента.

Если другой инструмент позже перезагружает классы, добавьте
`@RepeatInstrumentation` с тем же префиксом:

```java
@DeterministicCollections("com.example.myapp")
@RepeatInstrumentation({"com.example.myapp"})
```

## Ограничения

- `Map::ofEntries` и аналогичные method references компилируются в `invokedynamic` и не
  перехватываются. То же касается `HashMap::new` и `HashSet::new`.
- Анонимные подклассы `new HashMap<>() {{ ... }}` остаются подклассами `HashMap`:
  `NEW` создаёт анонимный класс, а его `super()`-вызов не переписывается.
- `ConcurrentHashMap`, `Hashtable`, `WeakHashMap`, `IdentityHashMap` не трогаются: смена
  реализации сломала бы их семантику.
- `TreeMap`, `TreeSet`, `EnumMap`, `EnumSet`, `LinkedHashMap`, `LinkedHashSet` уже
  детерминированы и не требуют замены.
- Порядок обхода `ConcurrentHashMap` остаётся недетерминированным; при необходимости
  сортируйте данные перед сериализацией.

## Рекомендации

1. Указывайте в `value()` только пакеты приложения, а не весь classpath: инструментация
   сканирует тела методов всех подходящих классов.
2. Не отключайте `MAP_OF`/`SET_OF`/`COPY_OF`/`COLLECTORS` надолго — это возвращает проблему,
   ради которой аннотация и создавалась.
3. Проверяйте не только порядок, но и контракт: имутабельность и реакции на `null`/дубликаты
   остаются JDK-шными.
