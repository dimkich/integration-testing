Кастомизация и расширение фреймворка через Java
================================================

[English version](../../en/redis/Extensibility.md)

Модуль `integration-testing-redis` спроектирован как расширяемая система. Если ваше приложение
использует проприетарные клиенты Redis, самописные протоколы сериализации или специфические модули
СУБД (например, RedisJSON), вы можете легко научить фреймворк работать с ними.

Все кастомные компоненты регистрируются как стандартные Spring-бины (`@Component` или `@Bean`)
и автоматически встраиваются в конвейер обработки данных фреймворка.

Собственные Кодеки и Схемы (`RedisDataCodec`, `RedisDataSchema`)
----------------------------------------------------------------

Если стандартных строковых или байтовых сериализаторов недостаточно, вы можете реализовать свои.

### Кастомный кодек (`RedisDataCodec`)

Кодек преобразует Java-объекты конкретного типа в байты и обратно. Например, кодек для сжатия строк
по алгоритму Snappy:

```java
package com.example.redis.codec;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import org.xerial.snappy.Snappy;

import java.nio.charset.StandardCharsets;

public class SnappyStringCodec implements RedisDataCodec {

    @Override
    public byte[] serialize(Object object) {
        if (object == null) return null;
        try {
            return Snappy.compress(object.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сжатия данных", e);
        }
    }

    @Override
    public Object deserialize(byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            byte[] uncompressed = Snappy.uncompress(data);
            return new String(uncompressed, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка распаковки данных", e);
        }
    }
}
```

### Кастомная схема (`RedisDataSchema`)

Схема объединяет кодеки для простых значений, ключей хэша и значений хэша. Вы можете написать свою
реализацию с нуля или воспользоваться готовым конструктором `ComposedRedisDataSchema`:

```java

@Configuration
public class MyTestConfig {

    @Bean
    public RedisDataSchema snappySchema() {
        RedisDataCodec snappy = new SnappyStringCodec();
        RedisDataCodec standardString = new StringRedisDataCodec();

        // Значения сжимаем через Snappy, а ключи полей хэша оставляем обычными строками
        return new ComposedRedisDataSchema(snappy, standardString, snappy);
    }
}
```

После регистрации бина в контексте вы можете ссылаться на него в `application-test.yml`:

```yaml
connections:
  redisConnectionFactory:
    schemas:
      "compressed:":
        bean-ref: "snappySchema"
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
import io.github.dimkich.integration.testing.redis.codec.RedisDataSchema;
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

Адаптеры кодеков и схем (`RedisDataCodecAdapter`, `RedisDataSchemaAdapter`)
---------------------------------------------------------------------------

Часто в приложении уже настроены сложные клиенты Redis (например, проприетарные обёртки над Jedis
или сторонние клиенты), которые хранят внутри себя правила сериализации. Чтобы не дублировать их
код в тестах, напишите **Адаптер**.

Адаптер обучает фабрику `RedisObjectFactory` на лету извлекать сериализаторы из ваших внутренних
бинов.

### Пример адаптера схемы для кастомного клиента

Предположим, в приложении объявлен бин вашего кастомного клиента:

```java
public class CustomRedisClient {
    private final MyCustomSerializer valueSerializer;
    private final MyCustomSerializer hashSerializer;
    // ...
}
```

Реализуем адаптер `RedisDataSchemaAdapter`, который преобразует этот клиент в понятную фреймворку
схему:

```java
package com.example.redis.adapter;

import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchemaAdapter;
import io.github.dimkich.integration.testing.redis.schema.ComposedRedisDataSchema;
import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import com.example.CustomRedisClient;
import org.springframework.stereotype.Component;

@Component // Адаптер автоматически регистрируется в реестре фреймворка
public class CustomClientSchemaAdapter implements RedisDataSchemaAdapter {

    @Override
    public RedisDataSchema tryCreateSchema(Object bean) {
        if (bean instanceof CustomRedisClient client) {
            // Создаем кодек-обертку для значений
            RedisDataCodec valueCodec = new RedisDataCodec() {
                @Override
                public byte[] serialize(Object obj) {
                    return client.getValueSerializer().toBytes(obj);
                }

                @Override
                public Object deserialize(byte[] bytes) {
                    return client.getValueSerializer().fromBytes(bytes);
                }
            };

            // Создаем кодек-обертку для хэшей
            RedisDataCodec hashCodec = new RedisDataCodec() {
                @Override
                public byte[] serialize(Object obj) {
                    return client.getHashSerializer().toBytes(obj);
                }

                @Override
                public Object deserialize(byte[] bytes) {
                    return client.getHashSerializer().fromBytes(bytes);
                }
            };

            return new ComposedRedisDataSchema(valueCodec, hashCodec, hashCodec);
        }
        return null; // Передаем ход другим адаптерам, если бин не нашего типа
    }
}
```

Теперь в YAML-конфигурации вы можете напрямую передать ваш бин `customRedisClient` в качестве схемы
по умолчанию или схемы для префикса:

```yaml
connections:
  redisConnectionFactory:
    defaultSchema:
      bean-ref: "customRedisClientBean"  # Будет успешно преобразован адаптером
```

Собственные теги бинарного формата (`BinarySegmentProvider`, `BinarySegment`)
-----------------------------------------------------------------------------

Если вам нужно упаковывать данные в кастомные бинарные заголовки (например, дописывать уникальные
хеш-суммы, подписи или динамические токены авторизации), вы можете добавить собственный тег
в синтаксис парсера `BinaryFormatParser`.

Для этого реализуйте интерфейс сегмента `BinarySegment` и его фабрику `BinarySegmentProvider`.

### Пример создания тега XOR-маскирования `{XORMASK(mask)}`

**1. Создаём сам сегмент (логику записи и чтения байт):**

```java
package com.example.redis.segment;

import io.github.dimkich.integration.testing.redis.codec.segment.BinarySegment;
import io.github.dimkich.integration.testing.redis.codec.segment.ReadContext;

import java.nio.ByteBuffer;

public class XorMaskSegment implements BinarySegment {
    private final byte mask;

    public XorMaskSegment(byte mask) {
        this.mask = mask;
    }

    @Override
    public void read(ByteBuffer buffer, ReadContext ctx) {
        byte maskedValue = buffer.get();
        byte originalValue = (byte) (maskedValue ^ mask);
    }

    @Override
    public void write(ByteBuffer buffer, byte[] payload) {
        byte originalValue = 0x5A;
        buffer.put((byte) (originalValue ^ mask));
    }

    @Override
    public int length() {
        return 1;
    }
}
```

**2. Регистрируем провайдер сегмента как `@Component`:**

```java
package com.example.redis.segment;

import io.github.dimkich.integration.testing.redis.codec.segment.BinarySegment;
import io.github.dimkich.integration.testing.redis.codec.segment.BinarySegmentProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component // Провайдер автоматически встраивается в парсер BinaryFormatParser
public class XorMaskSegmentProvider implements BinarySegmentProvider {

    @Override
    public String getName() {
        return "XORMASK"; // Имя тега для использования в YAML (регистр не важен)
    }

    @Override
    public BinarySegment create(List<String> params) {
        byte mask = (byte) Integer.decode(params.get(0)).intValue();
        return new XorMaskSegment(mask);
    }
}
```

После этого вы можете использовать тег `{XORMASK}` в настройках бинарных форматов:

```yaml
valueBinaryFormat: "{XORMASK(0xAA)}{LEN(SHORT, BE)}{CONTENT}"
```

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
