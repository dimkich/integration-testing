/**
 * Deterministic collection support: replaces order-sensitive JDK collection factories and
 * hash-based collection constructors with ordered analogues.
 *
 * <h2>How it works</h2>
 * <ol>
 *     <li>{@link io.github.dimkich.integration.testing.collections.DeterministicCollections}
 *         declares the class/package prefixes whose bytecode should be instrumented.</li>
 *     <li>{@link io.github.dimkich.integration.testing.collections.DeterministicCollectionsPlugin}
 *         is discovered through {@link java.util.ServiceLoader} and registers the transformations.</li>
 *     <li>{@link io.github.dimkich.integration.testing.collections.OrderedCollections} provides the
 *         replacement methods invoked from the instrumented bytecode.</li>
 *     <li>{@link io.github.dimkich.integration.testing.collections.HashCollectionRewriter} rewrites
 *         {@code new HashMap}/{@code new HashSet} allocations, which cannot be handled by
 *         Byte Buddy's member substitution alone.</li>
 * </ol>
 *
 * <p>Only the iteration order changes. Mutability, null handling and duplicate checks stay
 * identical to the JDK contracts.</p>
 */
package io.github.dimkich.integration.testing.collections;
