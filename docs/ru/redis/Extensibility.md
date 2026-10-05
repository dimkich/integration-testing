Кастомизация и расширение фреймворка через Java
================================================

[English version](../../en/redis/Extensibility.md)

Модуль `integration-testing-redis` спроектирован как расширяемая система. Если ваше приложение
использует проприетарные клиенты Redis, самописные протоколы сериализации или специфические модули
СУБД (например, RedisJSON), вы можете легко научить фреймворк работать с ними.

Все кастомные компоненты регистрируются как стандартные Spring-бины (`@Component` или `@Bean`)
и автоматически встраиваются в конвейер обработки данных фреймворка.

Собственные форматы сериализации
--------------------------------

Если стандартных core-провайдеров (`string`, `bytes`, `json`, ...) недостаточно, в качестве
источника компонента можно использовать свой Spring Data `RedisSerializer` или Redisson `Codec`,
либо написать собственный `TestSerdeAdapter` — см. раздел «Адаптеры источников» ниже.
`RedisDataCodec` — внутренний класс фреймворка (пара serde-конверторов), реализовывать его не нужно.

### Кастомный сериализатор (Spring Data `RedisSerializer`)

Сериализатор преобразует Java-объекты в байты и обратно. Например, сжатие строк по алгоритму
Snappy:

```java
package com.example.redis.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;
import org.xerial.snappy.Snappy;

import java.nio.charset.StandardCharsets;

public class SnappyStringSerializer implements RedisSerializer<Object> {

    @Override
    public byte[] serialize(@Nullable Object object) {
        if (object == null) return null;
        try {
            return Snappy.compress(object.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сжатия данных", e);
        }
    }

    @Override
    public Object deserialize(@Nullable byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            return new String(Snappy.uncompress(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка распаковки данных", e);
        }
    }
}
```

### Компонент схемы из кастомного сериализатора

Схема собирается из трёх компонентов (`value`/`hash-key`/`hash-value`), и источником каждого
может быть ваш сериализатор. Например, значения сжимаем через Snappy, а ключи полей хэша
оставляем обычными строками:

```yaml
connections:
  redisConnectionFactory:
    schemas:
      "compressed:":
        value:
          type: "com.example.redis.serializer.SnappyStringSerializer"
        hash-key:
          type: string
        hash-value:
          type: "com.example.redis.serializer.SnappyStringSerializer"
```

Кастомные стратегии записи данных (`RedisDataAccessor`)
---------------------------------------------------------

Если вы создали собственный тип данных, реализующий интерфейс `RedisValue` (например,
`CustomBloomFilter`), фреймворку нужна инструкция, как записывать его в Redis. Для этого
реализуйте `RedisDataAccessor<D>`.

### Пример: Accessor для кастомного типа

```java
package com.example.redis.accessor;

import io.github.dimkich.integration.testing.redis.accessor.RedisDataAccessor;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;
import io.github.dimkich.integration.testing.redis.model.RedisValue;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;

@Component  // Автоматически регистрируется в RedisAccessorCoordinator
public class BloomFilterAccessor implements RedisDataAccessor<CustomBloomFilter> {

    @Override
    public Class<? extends RedisValue> getSupportedClass() {
        return CustomBloomFilter.class;
    }

    @Override
    public void store(byte[] key, CustomBloomFilter value,
                      RedisConnection conn, RedisDataSchema schema) {
        conn.set(key, schema.getValueCodec().serialize(value));
    }
}
```

### Регистрация подтипа `RedisValue` в Jackson

Чтобы новый тип корректно десериализовывался из XML/JSON, зарегистрируйте его
в `TestSetupModule`:

```java

@Bean
TestSetupModule testSetupModule() {
    return new TestSetupModule()
            .addSubTypes(CustomBloomFilter.class, "CustomBloomFilter");
}
```

После этого вы сможете использовать `CustomBloomFilter` в XML-тестах по короткому имени.

Адаптеры источников (`TestSerdeAdapter`)
----------------------------------------

Часто в приложении уже настроены сложные клиенты Redis (например, проприетарные обёртки над Jedis
или сторонние клиенты), которые хранят внутри себя правила сериализации. Чтобы не дублировать их
код в тестах, напишите **адаптер**, который приводит нативный однослотовый источник (например,
Spring Data `RedisSerializer` или Redisson `Codec`) к системному контракту serde — паре
«объект ↔ byte[]». Общие
правила выбора адаптеров (обход иерархии классов, порядок кандидатов, `null` = «не моё») задаёт
`io.github.dimkich.integration.testing.serde.AdapterManager`.

Адаптер реализует `TestSerdeAdapter<S, I, O, C, R, P>`
и возвращает конвертер `input -> output`, собранный через `TestSerdeConverter.of(...)`.

### Пример: адаптер для кастомного сериализатора

Предположим, в приложении объявлен бин вашего кастомного сериализатора:

```java
public class CustomSerializer {
    public byte[] write(Object value) { ... }
    public Object read(byte[] data) { ... }
}
```

Реализуем адаптер сериализации:

```java
package com.example.redis.adapter;

import com.example.CustomSerializer;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component // Адаптер автоматически подхватывается менеджером адаптеров
@Getter
public class CustomSerializerAdapter
        implements TestSerdeAdapter<CustomSerializer, Object, byte[], TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<CustomSerializer> sourceClass = CustomSerializer.class;

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, byte[], TestSerdeContext> adapt(
            CustomSerializer source, StandardSerdeProperties properties, Class<Object> inputClass,
            Class<byte[]> outputClass, Class<TestSerdeContext> contextClass, ComponentRole role) {
        if (!byte[].class.equals(outputClass)) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> source.write(input));
    }
}
```

Симметричный адаптер `byte[] -> Object` вызывает `source.read(...)`. После регистрации бин можно
указывать в компоненте схемы:

```yaml
connections:
  redisConnectionFactory:
    defaultSchema:
      value:
        bean-ref: "customSerializerBean"
```

### Слоты Redisson-кодека

Если компонент схемы ссылается на Redisson `Codec` (напрямую или через `RedissonClient`), ядро
передаёт адаптеру роль слота, и тот извлекает нужную пару кодирования: для `value` —
`ValueEncoder`/`ValueDecoder`, для `hash-key` — `MapKeyEncoder`/`MapKeyDecoder`, для `hash-value` —
`MapValueEncoder`/`MapValueDecoder`. Поэтому один и тот же бин можно указать во всех трёх
компонентах схемы, и поля хэша будут сериализоваться своими кодеками:

```yaml
connections:
  redisConnectionFactory:
    schemas:
      "cache:":
        value:
          bean-ref: "redissonClient"
        hash-key:
          bean-ref: "redissonClient"
        hash-value:
          bean-ref: "redissonClient"
```

Собственные теги бинарной упаковки
----------------------------------

Кастомные теги (`BinarySegmentProvider`/`BinarySegment`) — общая возможность ядра serde: реализуйте
сегмент и его провайдер и зарегистрируйте провайдер как `@Component`. Пример создания тега
`{XORMASK(mask)}` — в
[Расширяемости Serde](../serde/Extensibility.md#свои-теги-бинарной-упаковки-binarysegmentprovider-binarysegment).


Кастомные обработчики команд репликации (`RedisSnapshotHandler`, `RedisStreamHandler`)
--------------------------------------------------------------------------------------

Если ваше приложение использует сложные системные команды Redis или специфические модули базы
данных (например, RedisJSON), фоновый поток репликации при встрече таких событий будет падать
с ошибкой `UnsupportedOperationException`.

Вы можете научить репликатор обрабатывать эти события, написав собственные обработчики.

### Кастомный парсер команд (`NamedCommandParser`)

Если Redis выполняет нестандартную или новую команду, отсутствующую в библиотеке
`redis-replicator`, зарегистрируйте для неё парсер команд:

```java
package com.example.redis.parser;

import com.moilioncircle.redis.replicator.cmd.impl.AbstractCommand;
import io.github.dimkich.integration.testing.redis.replication.NamedCommandParser;
import org.springframework.stereotype.Component;

@Component // Репликатор автоматически подключит этот парсер при старте
public class MyCustomCommandParser implements NamedCommandParser<MyCustomCommand> {

    @Override
    public String getCommandName() {
        return "MYCUSTOMCMD"; // Название команды в верхнем регистре
    }

    @Override
    public MyCustomCommand parse(Object[] command) {
        byte[] key = (byte[]) command[1];
        byte[] value = (byte[]) command[2];
        return new MyCustomCommand(key, value);
    }
}
```

Где `MyCustomCommand` — ваш класс команды, унаследованный от `AbstractCommand`.

### Обработчик живого стрима репликации (`RedisStreamHandler`)

После того как команда успешно распарсена, её необходимо применить к локальному In-Memory зеркалу
базы данных `RedisInMemoryStore`:

```java
package com.example.redis.handler;

import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import com.example.parser.MyCustomCommand;
import org.springframework.stereotype.Component;

@Component // Диспетчер событий автоматически зарегистрирует этот обработчик
public class MyCustomCommandHandler implements RedisStreamHandler<MyCustomCommand> {

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return eventClass == MyCustomCommand.class;
    }

    @Override
    public void handle(MyCustomCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            entry.setValue(schema, event.getValue());
        });
    }
}
```

### Обработчик снимков базы данных RDB (`RedisSnapshotHandler`)

Если кастомный тип данных должен поддерживаться не только в потоке команд, но и загружаться при
первоначальном получении полного снимка базы данных (RDB), вам необходимо реализовать обработчик
событий снимка:

```java
package com.example.redis.handler;

import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueModule;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSnapshotHandler;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import org.springframework.stereotype.Component;

@Component
public class MyModuleSnapshotHandler implements RedisSnapshotHandler<KeyStringValueModule> {

    @Override
    public boolean canHandle(Class<? extends Event> clazz) {
        return clazz == KeyStringValueModule.class;
    }

    @Override
    public void handle(KeyStringValueModule event, RedisInMemoryStore store) {
        store.compute(event, RedisHash.class, (schema, hash) -> {
            byte[] rawValue = event.getValue();
            // Ваша логика распаковки модуля и наполнения хэша...
            hash.putMapEntry("status", "LOADED_FROM_MODULE");
        });
    }
}
```

Все кастомные обработчики снимков (`RedisSnapshotHandler`) и стримов (`RedisStreamHandler`)
автоматически собираются диспетчерами событий `snapshotDispatcher` и `streamDispatcher`, полностью
устраняя проблему неподдерживаемых типов данных во время тестов.

---
[← На главную](../README.md)
