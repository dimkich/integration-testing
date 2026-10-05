Расширяемость serde
===================

[English version](../../en/serde/Extensibility.md)

Ядро serde расширяется четырьмя точками: фабрика провайдера формата, фабрика конвертеров для класса
конфига, адаптер и декоратор конвертера. Все они регистрируются как обычные Spring-бины
(`@Component` или `@Bean`) и подхватываются по своим дженерикам.

Свой формат (`TestSerdeProviderFactory`)
----------------------------------------

Фабрика провайдера создаёт конвертер `вход → выход` для запрошенной пары типов и регистрируется под
собственным именем — это имя указывается в `type`:

```java
package com.example.serde;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Getter
public class Base64SerdeProvider implements TestSerdeProviderFactory<byte[], Object, TestSerdeContext,
        ComponentRole, StandardSerdeProperties> {

    private final Class<byte[]> inputClass = byte[].class;
    private final Class<Object> outputClass = Object.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;
    private final String name = "base64";

    @Override
    public TestSerdeConverter<byte[], Object, TestSerdeContext> create(StandardSerdeProperties config,
                                                                       Class<byte[]> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       ComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, contextClass, (input, context) ->
                input == null ? null
                        : new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8));
    }
}
```

```yaml
"order-out":
  deserializer:
    value:
      type: base64
```

Правила:

* фабрика должна быть Spring-бином: если ваш тестовый пакет не попадает в сканирование, объявите
  её через `@Bean` в конфигурации теста;
* провайдер выбирается по паре «имя + класс конфига»: провайдер, зарегистрированный для подкласса
  конфига, не виден его предкам. Кандидаты упорядочены по специфичности класса конфига (подкласс
  специфичнее предка), и если провайдер подкласса не подошёл по направлению/роли/контексту,
  рассматриваются провайдеры предков — так же, как fallback у адаптеров;
* имена провайдеров сравниваются точно, с учётом регистра; два провайдера с одинаковыми именем,
  конфигом, направлением (`getInputClass()`/`getOutputClass()`) и ролью не конфликтуют, а образуют
  цепочку fallback-ов, как адаптеры: порядок задаётся специфичностью, при полном равенстве —
  порядком бинов (`@Order`);
* `getInputClass()`/`getOutputClass()` описывают пару типов провайдера: ядро сравнивает её с
  запрошенной строго (`equals`) — совместимость и наследование не учитываются, `null` означает
  «любой тип» и допустим только у реально универсальных компонентов; свободную (data) сторону
  объявляйте как `Object`, конкретные типы допустимы только у конвертеров из `beanRef`/FQCN
  (их приводят retype-адаптеры). Собирайте
  конвертер через `TestSerdeConverter.of(...)` с типами запроса — тогда он по построению
  удовлетворяет запросу; если пара `null/null` делает провайдера двунаправленным, проверяйте
  запрошенное направление в `create` и возвращайте `null`, когда пара не поддерживается;
* вернув не-`null` конвертер, провайдер обязан удовлетворять запросу (`satisfies`); иначе ядро
  бросает `IllegalArgumentException` — это признак неверно написанного провайдера. `null` —
  единственный способ сказать «не моё»;
* контекст (`C`) — часть контракта: при запросе с несовместимым контекстом конвертер не будет
  выбран. Для источников без транспорта (заголовки, части записи) берите `TestSerdeContext`;
* роль (`R`) — тоже часть контракта: платформенный enum можно проверять в `create`, чтобы
  вернуть разный конвертер для разных слотов источника (см. «Роли компонентов»);
* бинарный конверт накладывает ядро декораторами (`BinaryEnvelopeSerializerDecorator`/
  `BinaryEnvelopeDeserializerDecorator`) поверх готового конвертера — сама фабрика его не оборачивает.

Своя фабрика конвертеров (`TestSerdeConverterFactory`)
------------------------------------------------------

Фабрика выбирается по классу конфига, а не по имени в `type`, — например, когда у конфига вообще
нет `type`. Контракт — `TestSerdeConverterFactory<I, O, C, R, P>`; возвращается конвертер для
запрошенной пары типов.

```java
@Component
@Getter
public class MySerdeFactory
        implements TestSerdeConverterFactory<Object, Object, TestSerdeContext, ComponentRole, MySerdeProperties> {

    @Nullable
    private final Class<Object> inputClass = null;
    @Nullable
    private final Class<Object> outputClass = null;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<MySerdeProperties> propertiesClass = MySerdeProperties.class;

    @Override
    public TestSerdeConverter<Object, Object, TestSerdeContext> create(MySerdeProperties config,
                                                                       Class<Object> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       ComponentRole role) {
        // создайте конвертер по полям config
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> convert(config, input));
    }
}
```

Правила:

* фабрика выбирается по классу конфига или ближайшему предку: `ConverterManager` обходит цепочку
  суперклассов и интерфейсов;
* `getInputClass()`/`getOutputClass()` объявляют направление компонента: пара сравнивается с
  запрошенной строго (`equals`, `null` — «любой тип»); для компонента, умеющего оба направления,
  объявляйте `null/null`, а конкретную пару проверяйте в `create`, возвращая `null` для
  неподдерживаемых запросов;
* если `create` вернул не-`null` конвертер, он обязан удовлетворять запросу (`satisfies`); иначе
  ядро бросает `IllegalArgumentException` — это признак неверно написанной фабрики. `null` —
  единственный способ сказать «не моё»;
* `P` свободен: конфиг может быть собственным POJO и не расширять `StandardSerdeProperties`;
* на один класс конфига может быть несколько фабрик: они различаются направлением и ролью,
  а при полном равенстве специфичности сохраняется порядок бинов;
* декорация (бинарный конверт) — тоже задача ядра, а не фабрики.

Свой адаптер (`TestSerdeAdapter`)
---------------------------------

Адаптер — мост от уже разрешённого нативного источника к конвертеру `вход → выход`:
`TestSerdeAdapter<S, I, O, C, R, P>` превращает нативный бин (например, Kafka `Serializer` или
Spring Data `RedisSerializer`) в `TestSerdeConverter`.

Общие правила выбора:

* кандидаты упорядочены по специфичности: подтип источника идёт раньше предка, конкретная пара
  вход/выход — раньше `null`, явная роль — раньше универсальной;
* первый адаптер, вернувший не-`null`, побеждает. `null` означает «не моё», и менеджер пробует
  следующий; исключение из адаптера прерывает разрешение;
* порядок кандидатов детерминированный: специфичность (роль, источник, класс конфига, вход, выход,
  контекст); несравнимые классы упорядочиваются по числу супертипов, при полном равенстве
  сохраняется порядок бинов, переданных менеджеру;
* роль адаптера (`getRole()`) участвует в отборе: адаптер с явной ролью побеждает универсальный,
  а запрос без роли видит только универсальные адаптеры;
* несколько адаптеров на одну тройку образуют не конфликт, а последовательность fallback-ов;
* для конфига, не расширяющего `StandardSerdeProperties`, указывайте `Object` или точный класс —
  адаптер для `StandardSerdeProperties` к такому конфигу не применяется.

Пример (нативный сериализатор → core-конвертер):

```java
@Component
@Getter
public class NativeSerializerToCoreAdapter implements TestSerdeAdapter<
        NativeSerializer, Object, byte[], TestSerdeContext, ComponentRole, StandardSerdeProperties> {

    private final Class<NativeSerializer> sourceClass = NativeSerializer.class;
    private final Class<Object> inputClass = Object.class;
    private final Class<byte[]> outputClass = byte[].class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    public TestSerdeConverter<Object, byte[], TestSerdeContext> adapt(
            NativeSerializer source, StandardSerdeProperties properties,
            Class<Object> inputClass, Class<byte[]> outputClass, Class<TestSerdeContext> contextClass,
            ComponentRole role) {

        if (!byte[].class.equals(outputClass)) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> source.serialize(input));
    }
}
```

Конкретные примеры адаптеров — на страницах платформ: [Redis](../redis/Extensibility.md) (нативный
сериализатор → конвертер), [Kafka](../kafka/Serde.md) (core-конвертер → запись Kafka).

Роли компонентов (`ComponentRole`)
----------------------------------

Платформа может уточнять, какой именно слот данных запрашивается: например, Redis различает кодек
значения, ключа хэша и значения хэша. Ядро при этом не знает о слотах платформы: роль — это
generic-параметр `R extends ComponentRole` (такой же, как `C extends TestSerdeContext`), а
платформенный enum подставляется в `TestSerdeAdapter`/`TestSerdeConverterFactory`/
`TestSerdeProviderFactory`, поэтому `adapt`/`create` получают типизированную роль.

* `getRole()` — роль компонента; по умолчанию `null`: компонент универсален и подходит под любой
  запрос;
* в `matches(..., role)` компонент с заданной ролью подходит только при совпадающей запрошенной
  роли (сравнение по `name()`), универсальный — всегда;
* компонент с явно заданной ролью специфичнее универсального;
* роль входит в ключи кэшей `ConverterManager` и `AdapterManager`, поэтому разные слоты одного
  источника не смешиваются;
* у `SerdeManager` два входа, и роль передаётся в оба явно: `resolve(props, ..., role)` разрешает
  конвертер из конфигурации, а `adapt(source, props, ..., role)` адаптирует уже готовый источник;
  передайте `null`, если роль не нужна.

Свой декоратор конвертера (`TestSerdeDecoratorFactory`)
-------------------------------------------------------

Декоратор оборачивает уже разрешённый конвертер — так ядро накладывает бинарный конверт
(`BinaryEnvelopeSerializerDecorator`/`BinaryEnvelopeDeserializerDecorator`). Декоратор выбирается по
классу конфига, контексту и направлению: объявленная пара `getInputClass()`/`getOutputClass()`
сравнивается с запрошенной строго (`equals`, `null` — «любой тип»), иначе декоратор к этому
разрешению не применяется. Благодаря этому сериализующий декоратор получает только запросы
`* → byte[]`, а десериализующий — только `byte[] → *`; верните исходный конвертер, если
декоратор не применим по своей конфигурации.

```java
@Component
@Getter
public class MyDecoratorFactory implements TestSerdeDecoratorFactory<
        Object, Object, TestSerdeContext, StandardSerdeProperties> {

    @Nullable
    private final Class<Object> inputClass = null;
    @Nullable
    private final Class<Object> outputClass = null;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    public TestSerdeConverter<Object, Object, TestSerdeContext> decorate(
            TestSerdeConverter<Object, Object, TestSerdeContext> converter, StandardSerdeProperties config) {
        return new MyDecoratedConverter(converter);
    }
}
```

Свои теги бинарной упаковки (`BinarySegmentProvider`, `BinarySegment`)
---------------------------------------------------------------------

Если вам нужно упаковывать данные в кастомные бинарные заголовки (например, дописывать уникальные
хеш-суммы, подписи или динамические токены авторизации), вы можете добавить собственный тег
в синтаксис парсера `BinaryEnvelopeParser`.

Для этого реализуйте интерфейс сегмента `BinarySegment` и его фабрику `BinarySegmentProvider`.

### Пример создания тега XOR-маскирования `{XORMASK(mask)}`

**1. Создаём сам сегмент (логику записи и чтения байт):**

```java
package com.example.serde.segment;

import io.github.dimkich.integration.testing.serde.binary.BinarySegment;
import io.github.dimkich.integration.testing.serde.binary.ReadContext;

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
package com.example.serde.segment;

import io.github.dimkich.integration.testing.serde.binary.BinarySegment;
import io.github.dimkich.integration.testing.serde.binary.BinarySegmentProvider;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component // Провайдер автоматически встраивается в парсер BinaryEnvelopeParser
@Getter
public class XorMaskSegmentProvider implements BinarySegmentProvider {

    private final String name = "XORMASK"; // Имя тега для использования в YAML (регистр не важен)

    @Override
    public BinarySegment create(List<String> params) {
        byte mask = (byte) Integer.decode(params.get(0)).intValue();
        return new XorMaskSegment(mask);
    }
}
```

После этого вы можете использовать тег `{XORMASK}` в настройках бинарных форматов:

```yaml
value:
  bean-ref: "testValueSerializer"
  binary-envelope: "{XORMASK(0xAA)}{LEN(SHORT, BE)}{CONTENT}"
```

---
[← На главную](../README.md)
