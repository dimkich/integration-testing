Типы и serde
============

[English version](../../en/postgresql/Types-and-Serde.md)

Модуль преобразует значения PostgreSQL в обычные Java/XML-значения, чтобы их можно было сравнивать
в `dataStorageDiff`. Для большинства типов достаточно встроенных преобразователей. Для отдельных
колонок можно задать свой serde.

## Встроенные преобразования

| PostgreSQL                                               | Представление в diff / XML                |
|----------------------------------------------------------|-------------------------------------------|
| `text`, `varchar`, `char`                                | `String`                                  |
| `uuid`, `inet`, `xml`, `interval`, диапазоны, `tsvector` | `String`                                  |
| перечисления (`enum`)                                    | `String`                                  |
| `bit`, `varbit`                                          | `String` из `0` и `1`                     |
| `bool`                                                   | `Boolean`                                 |
| `int2`, `smallint`, `smallserial`                        | `Short`                                   |
| `int4`, `integer`, `serial`                              | `Integer`                                 |
| `int8`, `bigint`, `bigserial`                            | `Long`                                    |
| `float4`, `real`                                         | `Float`                                   |
| `float8`, `double precision`                             | `Double`                                  |
| `numeric`, `decimal`                                     | `BigDecimal`; `NaN`/`Infinity` — строками |
| `date`                                                   | `LocalDate`                               |
| `time`                                                   | `LocalTime`                               |
| `timestamp`                                              | `LocalDateTime`                           |
| `timestamptz`                                            | `LocalDateTime` в зоне теста              |
| `timetz`                                                 | `OffsetTime`                              |
| `json`, `jsonb`                                          | объект, массив или скаляр                 |
| `hstore`                                                 | `Map`                                     |
| массивы                                                  | `List`, включая вложенные массивы         |
| составные типы                                           | `Map`                                     |
| `bytea`                                                  | `byte[]`; в XML — Base64                  |

Домены раскрываются до базового типа. Если для типа нет специального преобразователя, значение
остаётся строкой.

### Время

`timestamptz` приводится к зоне, которую использует `@MockJavaTime`, либо к UTC, если время не
мокается. В XML значение имеет тип `LocalDateTime`:

```xml
<ts_tz type="LocalDateTime">2026-01-01T10:15:30.123456</ts_tz>
```

`timetz` сохраняет смещение и имеет тип `OffsetTime`. В DBUnit-фикстурах время лучше задавать с
явным смещением.

### JSON и JSONB

`json`/`jsonb` разбираются Jackson-ом. Объект становится map, массив — списком, скаляр — обычным
значением. Числа с плавающей точкой разбираются как `BigDecimal`.

Если нужен конкретный DTO, укажите `target-class`:

```yaml
columns:
  "[public.events.payload]":
    type: json
    target-class: com.example.EventPayload
```

### Массивы

Массивы представляются `ArrayList`. Поддерживаются многомерные массивы, `NULL`-элементы, кавычки и
экранирование. Нижняя граница массива в diff не отображается — важен порядок элементов.

```xml
<tags type="ArrayList">
    <tags>tag_a</tags>
    <tags>tag_b</tags>
    <tags xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
</tags>
```

### Составные типы

Составной тип (`composite`) превращается в map по именам полей. `NULL`-поля отображаются как `nil`:

```xml
<addr type="MapStringKeyObjectValue">
    <city xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    <zip>220000</zip>
</addr>
```

### BYTEA

`bytea` декодируется в `byte[]` и в XML показывается в Base64:

```xml
<bin type="byte[]">BAUG</bin>
```

Пустой массив байт выглядит как пустой тег:

```xml
<bin type="byte[]"></bin>
```

Модуль ожидает стандартный `bytea_output = 'hex'`. При escape-формате значение может остаться
текстом.

## Свои преобразования для колонок

Настройки serde задаются в `columns` подключения. Ключ — `table.column` или
`schema.table.column`:

```yaml
integration:
  testing:
    postgresql:
      connections:
        dataSource:
          columns:
            "[public.spi_values.location]":
              type: it-point
            "[public.serde_values.data]":
              type: json
              target-class: com.example.UserData
            "[public.serde_values.payload]":
              type: string
              binary-envelope: "{VER(1)}{LEN(INT, BE)}{CONTENT}"
```

Способы задать источник:

* `type` без точки — имя зарегистрированного провайдера, например `it-point`;
* `type` с точкой — полное имя класса;
* `bean-ref` — имя готового Spring-бина.

Поля `target-class` и `object-mapper-ref` работают с Jackson-провайдерами (`json`, `xml`, `yaml`).
`binary-envelope` накладывается поверх выбранного источника; формат описан в
[бинарных упаковках](../serde/Binary-Envelopes.md).

Для `bytea` сначала выполняется декодирование PostgreSQL, а затем вызывается ваш конвертер. Поэтому
кастомный конвертер для `bytea` получает уже бинарные данные, а не текст `\x...`.

### Свой провайдер

Если нужен новый формат, зарегистрируйте `TestSerdeProviderFactory` как Spring-бин. Для
PostgreSQL контекст — `PostgresColumnSerdeContext`, а роль — `PostgresTypeDescriptor`:

```java
@Component
@Getter
public class PointSerdeProvider implements TestSerdeProviderFactory<ByteBuffer, Object,
        PostgresColumnSerdeContext, PostgresTypeDescriptor, StandardSerdeProperties> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;
    private final Class<Object> outputClass = Object.class;
    private final Class<PostgresColumnSerdeContext> contextClass = PostgresColumnSerdeContext.class;
    private final Class<PostgresTypeDescriptor> roleClass = PostgresTypeDescriptor.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "it-point";

    @Override
    public TestSerdeConverter<ByteBuffer, Object, PostgresColumnSerdeContext> create(
            StandardSerdeProperties config, Class<ByteBuffer> inputClass, Class<Object> outputClass,
            Class<PostgresColumnSerdeContext> contextClass, PostgresTypeDescriptor role) {
        return TestSerdeConverter.of(ByteBuffer.class, Object.class, PostgresColumnSerdeContext.class,
                (input, context) -> StandardCharsets.UTF_8.decode(input.duplicate()).toString());
    }
}
```

Если роль не нужна, можно объявить `ComponentRole` — как в общих примерах
[расширяемости Serde](../serde/Extensibility.md). В `PostgresColumnSerdeContext` доступны имя таблицы,
имя колонки, SQL-тип и зона времени.

### Приоритет конвертеров

Если для колонки настроен свой конвертер, он считается авторитетным: встроенный конвертер не
подставляется как fallback. Если кастомный конвертер вернул `null`, значение считается `null`.

Если конвертация падает, сообщение содержит таблицу, колонку и SQL-тип:

```text
Failed to convert PostgreSQL column public.events.payload of type jsonb
```

В этом случае проверьте `columns` для этой колонки и соответствие `target-class` реальному JSON.

## Исключения и serde

`ignore: true` исключает колонку из diff ещё до конвертации. `excludedFields` применяется после
конвертации и удаляет только указанные вложенные поля. Аудит и откат при этом используют полное
значение.

---
[← На главную](../README.md)
