Serde Extensibility
===================

[Russian version](../../ru/serde/Extensibility.md)

The serde core has four extension points: a format provider factory, a converter factory for a config
class, an adapter and a converter decorator. All of them are registered as regular Spring beans
(`@Component` or `@Bean`) and are picked up by their generics.

A custom format (TestSerdeProviderFactory)
------------------------------------------

A provider factory creates an `input → output` converter for the requested type pair and is registered
under its own name — that name is used in `type`:

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

Rules:

* the factory must be a Spring bean: if your test package is not component-scanned, declare it via
  `@Bean` in the test configuration;
* a provider is selected by the "name + config class" pair: a provider registered for a config
  subclass is not visible to its ancestors. Candidates are ordered by config-class specificity
  (a subclass is more specific than its ancestor), and when a subclass provider does not fit the
  direction/role/context, ancestor providers are tried as a fallback, like adapters;
* provider names are matched exactly, case-sensitively; two providers with the same name, config,
  direction (`getInputClass()`/`getOutputClass()`) and role do not conflict but form a fallback
  chain, like adapters: ordered by specificity, and by bean order (`@Order`) when fully equal;
* `getInputClass()`/`getOutputClass()` describe the provider's type pair: the pair is matched by
  the Liskov substitution principle — the declared input must be a supertype of the requested
  input and the declared output a subtype of the requested output; `null` means "any type" and is
  allowed only for really universal components. Declare the free (data) side as `Object`, and use
  concrete types only for converters bound via `beanRef`/FQCN (retype adapters bridge them). Build
  the converter via `TestSerdeConverter.of(...)` with the request's types — then it satisfies the
  request by construction; if a `null/null` pair makes the provider bidirectional, check the
  requested direction in `create` and return `null` when a concrete pair is not supported;
* a provider that returns a non-`null` converter must satisfy the request (`satisfies`): the
  converter may declare a wider input or a narrower output than the request; otherwise the core
  throws an `IllegalArgumentException`, a sign of a wrongly written provider. `null` is the only way
  to say "not mine";
* the context (`C`) is part of the contract: a converter with an incompatible context is never
  selected. For transport-free sources (headers, record parts) use `TestSerdeContext`;
* the role (`R`) is also part of the contract: a platform enum may be inspected in `create` to
  return a different converter per source slot (see "Component roles");
* the binary envelope is applied by the core decorators (`BinaryEnvelopeSerializerDecorator`/
  `BinaryEnvelopeDeserializerDecorator`) on top of the resolved converter — the factory never wraps
  anything itself.

A custom converter factory (TestSerdeConverterFactory)
------------------------------------------------------

This factory is selected by the config class rather than by a `type` name — for example when the
config has no `type` at all. The contract is `TestSerdeConverterFactory<I, O, C, R, P>`; it returns a
converter for the requested type pair.

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
        // build a converter from config fields
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> convert(config, input));
    }
}
```

Rules:

* the factory is selected by the config class or its closest ancestor: `ConverterManager` walks the
  superclass and interface chain;
* `getInputClass()`/`getOutputClass()` declare the component direction: the pair is matched by the
  Liskov substitution principle — the declared input must be a supertype of the requested
  input and the declared output a subtype of the requested output; `null` means any type. For a
  component that handles both directions declare `null/null` and validate the concrete pair in
  `create` by returning `null` for unsupported requests;
* if `create` returns a non-`null` converter, it must satisfy the request (`satisfies`): the
  converter may declare a wider input or a narrower output than the request; otherwise the core
  throws an `IllegalArgumentException`, a sign of a wrongly written factory. `null` is the only way
  to say "not mine";
* `P` is free: the config may be a custom POJO and does not have to extend `StandardSerdeProperties`;
* several factories may share the same config class: they are distinguished by direction and role,
  and when specificity is equal the bean order is preserved;
* decoration (the binary envelope) is also the core's job, not the factory's.

A custom adapter (TestSerdeAdapter)
-----------------------------------

An adapter is a bridge from an already resolved native source to an `input → output` converter:
`TestSerdeAdapter<S, I, O, C, R, P>` turns a native bean (e.g. a Kafka `Serializer` or a Spring Data
`RedisSerializer`) into a `TestSerdeConverter`.

Common selection rules:

* candidates are ordered by specificity: a subtype source goes before its ancestors, a concrete
  input/output pair before `null`, an explicit role before a universal one;
* the input/output pair is matched by the Liskov substitution principle, as for providers and
  factories: the declared input must be a supertype of the requested input and the declared output a
  subtype of the requested output;
* the first adapter returning non-`null` wins. `null` means "not mine" and the manager tries the
  next one; an exception aborts the resolution;
* the candidate order is deterministic: specificity (the role, source, config class, input, output
  and context); unrelated classes are ordered by their number of supertypes, and ties keep the
  order of the beans passed to the manager;
* the adapter role (`getRole()`) takes part in the selection: an adapter with an explicit role wins
  over a universal one, and a request without a role sees only universal adapters;
* several adapters for the same triple form a fallback sequence, not a conflict;
* for a config that does not extend `StandardSerdeProperties`, declare `Object` or the exact class —
  an adapter declared for `StandardSerdeProperties` does not apply to it.

Example (native serializer → core converter):

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

Concrete adapter examples live on the platform pages: [Redis](../redis/Extensibility.md) (a native
serializer → a converter), [Kafka](../kafka/Serde.md) (a core converter → a Kafka record).

Component roles (ComponentRole)
-------------------------------

A platform may qualify which data slot is requested: for example, Redis distinguishes the codecs of
a value, a hash field name and a hash field value. The core knows nothing about platform slots: the
role is a generic parameter `R extends ComponentRole` (just like `C extends TestSerdeContext`), and
a platform enum is bound in `TestSerdeAdapter`/`TestSerdeConverterFactory`/
`TestSerdeProviderFactory`, so `adapt`/`create` receive a typed role.

* `getRole()` is the component role; the default is `null`: the component is universal and fits any
  request;
* in `matches(..., role)` a component with a role fits only a matching requested role (compared by
  `name()`), while a universal one always fits;
* a component with an explicit role is more specific than a universal one;
* the role is part of the `ConverterManager`/`AdapterManager` cache keys, so different slots of the
  same source never mix;
* `SerdeManager` has two entry points, and the role is passed to both explicitly:
  `resolve(props, ..., role)` resolves a converter from the configuration, while
  `adapt(source, props, ..., role)` adapts an already available source; pass `null` when no role
  applies.

A custom converter decorator (TestSerdeDecoratorFactory)
--------------------------------------------------------

A decorator wraps an already resolved converter — this is how the core applies the binary envelope
(`BinaryEnvelopeSerializerDecorator`/`BinaryEnvelopeDeserializerDecorator`). A decorator is selected by
the config class, context and direction, and unlike converters it is invariant: the declared
`getInputClass()`/`getOutputClass()` pair must be equal to the requested one (`equals`, `null` means
any type), otherwise the decorator is not applied to that resolution. That is why the serializing
decorator only receives `* → byte[]` requests and the deserializing one only `byte[] → *`; return the
original converter when the decorator is not applicable per its configuration.

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

Concrete adapter examples are on the platform pages: [Redis](../redis/Extensibility.md) (a native
client → `RedisDataSchema`), [Kafka](../kafka/Serde.md) (a core converter → a Kafka record).

Custom Binary Envelope Tags (`BinarySegmentProvider`, `BinarySegment`)
--------------------------------------------------------------------

If you need to wrap data in custom binary headers (e.g., unique hash sums, signatures,
or dynamic authorization tokens), you can add a custom tag to the `BinaryEnvelopeParser`
syntax.

To do this, implement the segment interface `BinarySegment` and its factory
`BinarySegmentProvider`.

### Example: Creating the `{XORMASK(mask)}` Tag

**1. Create the segment (read/write byte logic):**

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

**2. Register the segment provider as `@Component`:**

```java
package com.example.serde.segment;

import io.github.dimkich.integration.testing.serde.binary.BinarySegment;
import io.github.dimkich.integration.testing.serde.binary.BinarySegmentProvider;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component // Provider is automatically wired into the BinaryEnvelopeParser
@Getter
public class XorMaskSegmentProvider implements BinarySegmentProvider {

    private final String name = "XORMASK"; // Tag name for use in YAML (case-insensitive)

    @Override
    public BinarySegment create(List<String> params) {
        byte mask = (byte) Integer.decode(params.get(0)).intValue();
        return new XorMaskSegment(mask);
    }
}
```

After this, you can use the `{XORMASK}` tag in binary envelope settings:

```yaml
value:
  bean-ref: "testValueSerializer"
  binary-envelope: "{XORMASK(0xAA)}{LEN(SHORT, BE)}{CONTENT}"
```

---
[← Back to Home](../README.md)
