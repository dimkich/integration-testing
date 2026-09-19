/**
 * In-flight message tracking: detects messages that producers have sent to Kafka but
 * that consumers have not consumed yet.
 *
 * <p>Lag checking through the Admin API is eventually consistent and cannot see
 * messages still inside open transactions, so a test could finish before all messages
 * are delivered. This package therefore tracks sends and commits directly, from inside
 * the Kafka clients:</p>
 * <ul>
 *   <li>{@code ledger} — the state: per-topic pending sends, end and committed offsets,
 *       subscriptions and consumer liveness ({@code InFlightLedger}, {@code ClusterState});</li>
 *   <li>{@code agent} — Byte Buddy advices that register new producers/consumers and
 *       their subscriptions when clients are constructed or closed, track partition
 *       assignments through wrapped rebalance listeners and manual {@code assign},
 *       installed by {@code KafkaInFlightAgent};</li>
 *   <li>{@code interceptor} — Kafka client interceptors injected into the client
 *       configurations, reporting sends, acknowledgements and commits.</li>
 * </ul>
 *
 * <p>Tracking is enabled per test via {@code @EnableTestKafka(inflight = true)}. The
 * ledger is exposed to the wait-completion logic through the {@code KafkaStateChecker}
 * contract.</p>
 */
package io.github.dimkich.integration.testing.kafka.inflight;
