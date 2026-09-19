package io.github.dimkich.integration.testing.kafka.inflight.ledger;

/**
 * Typed identity of one consumer instance. Both fields are {@code null}-safe:
 * {@code bootstrapServers} is always set for a registered consumer,
 * {@code groupId} may be {@code null} if the underlying config lacked a group id.
 *
 * <p>Package-private: internal to the inflight ledger.
 */
record ConsumerIdentity(String bootstrapServers, String groupId) {
}