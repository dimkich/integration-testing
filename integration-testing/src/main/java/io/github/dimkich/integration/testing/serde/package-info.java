/**
 * Serialization subsystem: a single way to describe "how to serialize" for all
 * modules.
 *
 * <h2>Three levels</h2>
 *
 * <ol>
 *   <li><b>Leaf</b> — {@link io.github.dimkich.integration.testing.serde.TestSerdeSerializer} /
 *       {@link io.github.dimkich.integration.testing.serde.TestSerdeDeserializer}.
 *       Pure functions {@code T → byte[]} and {@code byte[] → T}. No Spring, no state.</li>
 *   <li><b>Provider</b> — {@link io.github.dimkich.integration.testing.serde.TestSerdeProvider}.
 *       A factory for a (serializer, deserializer) pair by format name. Knows about
 *       {@code targetClass} / {@code objectMapperRef} and can apply them.</li>
 *   <li><b>Adapter</b> — {@link io.github.dimkich.integration.testing.serde.TestSerdeAdapter}.
 *       A bridge between types when a provider returns something different from what the
 *       module needs. Located by a BFS over the class hierarchy.</li>
 * </ol>
 *
 * <h2>Transport context</h2>
 *
 * <p>Core interfaces ({@code TestSerdeSerializer}, {@code TestSerdeDeserializer}) work
 * without any transport context: they know nothing about topic, partition, headers,
 * Redis key, etc. This is a deliberate decision — it lets one provider ({@code json})
 * work in Kafka, Redis and any other module without changes.
 *
 * <p>If serialization <b>depends</b> on the transport context (topic, headers), use the
 * platform interface directly:
 * <ul>
 *   <li>in Kafka — {@code KafkaRecordSerializer} (via the record-level config);</li>
 *   <li>in Redis — {@code RedisDataCodec} (via {@code beanRef});</li>
 *   <li>or return your own type from a provider and register an adapter.</li>
 * </ul>
 *
 * <p>The {@code CoreTo*} / {@code *ToCore} adapters lose context intentionally — they
 * exist only so that context-free providers can work in modules where context exists.
 *
 * <p><b>Do not</b> fight this by extending the core interfaces with transport context,
 * by hiding context in a ThreadLocal, or by "smartly" detecting at runtime whether an
 * implementation needs the context (reflection over {@code serialize(String, Headers, T)}
 * and fallbacks). The rule is simple and unambiguous: if you need context — use the
 * record level (or any platform interface).
 *
 * <h2>Why there are many adapters — and why this is right</h2>
 *
 * <p><b>Do not try to fold adapters into module factories.</b> It looks like an
 * optimization (fewer classes), but it breaks the main feature of the system:
 *
 * <blockquote>
 * The user writes <b>one</b> provider — and gets a working <b>context-free value</b>
 * serializer in any module (Kafka, Redis, Web) without a single adapter and without
 * any knowledge of the platform.
 * </blockquote>
 *
 * <p>That is achieved exactly by chains of adapters. Example for Kafka:
 * <pre>
 * MySerializer (implements TestSerdeSerializer)
 *   ↓ CoreToKafkaSerializerAdapter
 * Serializer
 *   ↓ SerializerToKafkaRecordSerializerAdapter
 * KafkaRecordSerializer (with key, value, headers)
 * </pre>
 *
 * <p>This chain assembles a record serializer whose <b>value</b> is the user's
 * context-free serializer; key and headers come from the module defaults. One
 * provider covers context-free value serialization. For context-aware
 * serialization (headers, type info), use a record-level provider (see
 * {@code spring-json}, {@code spring-xml}) or implement {@code KafkaRecordSerializer}
 * directly.
 *
 * <p>If the chain is folded into a single factory, the user's provider stops
 * entering it automatically — the user would have to write their own adapter or
 * implement a platform interface. That is a <b>worse DX</b> in exchange for
 * "fewer lines in the repository".
 *
 * <h2>What to do if adapters seem "too many"</h2>
 *
 * <p>The right reaction is not to delete them but to <b>improve visibility</b>:
 * <ul>
 *   <li>a DEBUG log on the resolution path — which provider fired, which adapter chain
 *       was built;</li>
 *   <li>clear error messages — "no adapter from X to Y, supported paths: [...]";</li>
 *   <li>a metric: how many user lines each transit adapter saves. Usually one adapter
 *       is minus 10–20 lines in every user project.</li>
 * </ul>
 *
 * <h2>Entry points</h2>
 *
 * <ul>
 *   <li>{@link io.github.dimkich.integration.testing.serde.resolver.SerdeResolver} —
 *       provider or {@code beanRef}.</li>
 *   <li>{@link io.github.dimkich.integration.testing.serde.adapter.SerdeAdapterResolver} —
 *       adaptation to the target type.</li>
 *   <li>{@link io.github.dimkich.integration.testing.serde.SerdeManager} —
 *       facade for modules. Modules should depend only on it, not on the resolvers.</li>
 * </ul>
 *
 * <h2>How a module plugs in</h2>
 *
 * <p>A module (Kafka, Redis, ...) in its own config:
 * <ul>
 *   <li>declares a {@link io.github.dimkich.integration.testing.serde.SerdeProperties}
 *       subclass for its needs (e.g. the Kafka record config);</li>
 *   <li>registers {@code TestSerdeAdapter}s for bridges between the core types
 *       ({@code TestSerdeSerializer}, {@code TestSerdeDeserializer}) and its own
 *       (Kafka {@code Serializer}, Redis codecs);</li>
 *   <li>optionally registers module-specific {@code TestSerdeProvider}s for
 *       platform-specific formats (e.g. {@code spring-json});</li>
 *   <li>calls {@code SerdeManager.resolveAndAdaptSerializer/Deserializer} with
 *       the target type and a default.</li>
 * </ul>
 *
 * <p>A new format (Protobuf, Avro) is just a new {@code TestSerdeProvider} in
 * the core or in a module. Adding a new transport means adding a new set of
 * adapters. The core does not change.
 *
 * <h2>What NOT to optimize here</h2>
 *
 * <p>If something looks foldable — first check whether folding would break the
 * "the user wrote only a provider" scenario for context-free value serialization.
 * If it would, do not fold it.
 *
 * <h2>The context boundary: what the core can and cannot do</h2>
 *
 * <p>The core interfaces are deliberately context-free:
 * {@code TestSerdeSerializer} is {@code T → byte[]},
 * {@code TestSerdeDeserializer} is {@code byte[] → T}. No topic, no key,
 * no headers. This is not a limitation to be worked around — it is the
 * property that lets one provider serve every module (Kafka, Redis, Web)
 * without change.
 *
 * <p><b>What the core covers.</b> The vast majority of serialization needs:
 * a payload of a known type, serialized to bytes and back. Per-topic
 * behaviour is expressed at configuration level, not at serialization level:
 *
 * <pre>
 * topics:
 *   "billing-.*":
 *     serializer:   { type: billing-json }
 *   "audit-.*":
 *     serializer:   { type: audit-json }
 * </pre>
 *
 * <p><b>What the core does not cover, and why.</b> Three cases fall outside
 * the context-free contract. Each has a designated place, and none of them
 * justifies extending {@code TestSerdeSerializer} or
 * {@code TestSerdeDeserializer}.
 *
 * <ul>
 *   <li><b>Type information on deserialization.</b> Polymorphic reading
 *       requires the target {@code JavaType} <i>before</i> {@code readValue}
 *       is called. Metadata therefore influences the read itself, not the
 *       result of it. A context-free deserializer cannot receive this
 *       information — by construction, not by accident. This is why
 *       {@code spring-json} and {@code spring-xml} return
 *       {@code KafkaRecordDeserializer} directly, bypassing the core
 *       interfaces: they need the record's headers to pick a type, and the
 *       core interface has no channel to deliver them.</li>
 *
 *   <li><b>Metadata that depends on the transport.</b> Schema Registry
 *       lookups ({@code TopicNameStrategy}), per-topic encryption, and
 *       broker-specific routing all need the topic <i>at serialize time</i>.
 *       A context-free serializer does not see the topic. These are
 *       record-aware concerns, and they live in a record-aware provider
 *       (a {@code KafkaRecordSerializer} implementation inside the Kafka
 *       module), not in the core.</li>
 *
 *   <li><b>Metadata that depends on the transport context but not on the
 *       payload.</b> Request ids, tenant ids, trace ids. These are typically
 *       injected by an interceptor or a header mapper, not by the payload
 *       serializer. Putting them into the serializer would make the
 *       serializer itself transport-aware for no benefit.</li>
 * </ul>
 *
 * <p><b>The context boundary.</b> Core interfaces are parameterized by
 * {@link io.github.dimkich.integration.testing.serde.SerdeContext} — a marker interface with no
 * methods. This lets a single provider (e.g. {@code json}) work in any module without change:
 * it receives {@code SerdeContext} and ignores it.</p>
 *
 * <p>Platform modules extend {@code SerdeContext} with their own typed
 * accessors. A Kafka provider receives {@code KafkaSerdeContext} with
 * {@code getTopic()} and {@code getHeaders()}. Bridge adapters between
 * core and platform types use {@code ? extends SerdeContext} wildcards
 * so that both context-free and context-aware providers pass through the
 * same BFS chain.</p>
 */
package io.github.dimkich.integration.testing.serde;