package io.github.dimkich.integration.testing.serde;

/**
 * A bridge between two types: {@code S → T}. Needed when a provider returns one
 * type but the module requires another.
 *
 * <p><b>How an adapter differs from a provider:</b>
 * <ul>
 *   <li>A provider knows the <i>format</i> ({@code json}, {@code xml}). It
 *       builds a serializer/deserializer from the config.</li>
 *   <li>An adapter knows the <i>type pair</i>. It transforms an already built
 *       object. The config is needed only for assembly details (e.g. extracting
 *       key/headers when building a Kafka record serializer).</li>
 * </ul>
 *
 * <h2>Why adapters, not one big factory</h2>
 *
 * <p>At first glance N adapters may look like duplication that could be folded
 * into a single module factory. That is <b>not the case</b>. Adapters are
 * <b>decomposition</b>, and it buys three things:
 *
 * <ol>
 *   <li><b>The user writes less.</b> A provider returns a {@code TestSerdeSerializer},
 *       and adapters bring it to a {@code KafkaRecordSerializer}, a Redis codec
 *       or any other type. The user does not write a single adapter.</li>
 *
 *   <li><b>One provider — many modules.</b> {@code JsonSerdeProvider} in the core
 *       serves Kafka, Redis and Web because every module has adapters from the
 *       core types to its own. Without adapters you would have to duplicate the
 *       provider or put a shared interface in the core for each platform.</li>
 *
 *   <li><b>Extensibility without touching the core.</b> A new format — a new
 *       provider. A new transport — a new module with adapters. The core does
 *       not change.</li>
 * </ol>
 *
 * <p>So "transit" adapters such as {@code *ToKafkaRecordSerializerAdapter} are a
 * <b>feature, not a bug</b>. They let the user write a single provider and get a
 * working record serializer with all fields (key, value, headers) without a
 * single line of Kafka-side code.
 *
 * <h2>When to write a new adapter</h2>
 *
 * <ul>
 *   <li>A new module — a set of adapters between the core types
 *       ({@code TestSerdeSerializer}, {@code TestSerdeDeserializer}) and the module types.</li>
 *   <li>A third-party interface that you do not control — a wrapper to your own type.</li>
 *   <li>Custom conversion logic that does not exist in the core.</li>
 * </ul>
 *
 * <h2>What an adapter must NOT do</h2>
 *
 * <p>An adapter is <b>one conversion of one type into another</b>. If you find
 * yourself doing several steps inside an adapter (assembling a composite object,
 * resolving nested serializers), that is a module factory's job, not an
 * adapter's. For example, the "value + headers + key → record" composition lives
 * in {@code KafkaObjectFactory}; an adapter only brings a single component to the
 * required type.
 *
 * <p>But if composition is needed <b>only as a path to the target type</b> (the
 * user must not know about it), it may stay as a chain of adapters — as it is in
 * Kafka. This is acceptable because the value (the user writes only a provider)
 * outweighs the cost (a few transit classes).
 *
 * <p><b>Runtime flow.</b> {@link SerdeManager} first resolves a raw
 * {@code Object} via the serde resolver, then the adapter resolver searches a
 * path from the real class of that {@code Object} to the requested target type.
 * The search is a BFS over the class hierarchy (superclasses and interfaces),
 * because adapters are registered against interfaces rather than concrete
 * classes. One registration serves many lookups.
 *
 * <p><b>How to add a custom adapter:</b>
 * <ol>
 *   <li>Implement {@code TestSerdeAdapter<Source, Target, Props>}.</li>
 *   <li>Register it as a Spring bean — the adapter scanner finds it through the
 *       generic resolver by its generics.</li>
 *   <li>Check that the pair ({@code Source}, {@code Target}) does not conflict
 *       with an existing one — duplicates are detected at startup.</li>
 * </ol>
 *
 * <h3>Overriding built-in adapters</h3>
 *
 * <p>Registering an adapter for a (source, target, config) triple that a built-in adapter
 * already occupies is rejected at startup by {@code AdapterScanner} with
 * "Overriding built-in adapters is not supported". This is intentional: the built-in
 * chain is part of the framework's public contract, and silently replacing it would make
 * the behaviour of a user's provider depend on another user's beans.
 *
 * <p>If you need different behaviour for a standard source type, prefer one of:
 * <ul>
 *   <li><b>Introduce a subtype</b> of the source type and register the adapter for the subtype.
 *       The BFS visits the subtype first, so your adapter wins without touching built-ins.</li>
 *   <li><b>Introduce a subtype of the config class</b> for your module
 *       (e.g. {@code MyRecordProperties extends RecordProperties}). Adapters sharing the same
 *       target but with a stricter config class dominate at {@code pick}.</li>
 *   <li><b>Return a custom intermediate type</b> from your provider and register an adapter
 *       from that type to the platform type.</li>
 * </ul>
 *
 * @param <S> source type (what a provider or a previous adapter returned)
 * @param <T> target type (what the module needs)
 * @param <P> config class; the adapter applies only if {@code props} is compatible
 * @see SerdeManager
 * @see io.github.dimkich.integration.testing.serde.adapter.SerdeAdapterResolver
 */
@FunctionalInterface
public interface TestSerdeAdapter<S, T, P extends SerdeProperties> {
    /**
     * Converts the source object to the target type.
     *
     * @param source the object produced by a provider or by a previous adapter
     * @param properties the serde configuration; the adapter is selected only if
     *                   this configuration is compatible with {@code P}
     * @return the converted object
     */
    T adapt(S source, P properties);
}
