package io.github.dimkich.integration.testing.kafka.inflight.ledger;

/**
 * Typed identity of one producer instance. Both fields are {@code null}-safe:
 * {@code bootstrapServers} is always set for a registered producer,
 * {@code transactionalId} is set only for transactional producers.
 *
 * <p>Package-private: internal to the inflight ledger.
 */
record ProducerIdentity(String bootstrapServers, String transactionalId) {
}