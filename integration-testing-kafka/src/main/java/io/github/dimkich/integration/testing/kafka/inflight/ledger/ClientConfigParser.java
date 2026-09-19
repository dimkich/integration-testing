package io.github.dimkich.integration.testing.kafka.inflight.ledger;

import io.github.dimkich.integration.testing.kafka.util.BootstrapUtil;
import lombok.Value;

import java.util.Map;

/**
 * Extracts client configuration (bootstrap servers, transactional id, group id) from
 * {@code KafkaProducer} and {@code KafkaConsumer} constructor arguments.
 */
public class ClientConfigParser {

    /**
     * Producer client configuration relevant to the ledger.
     */
    @Value
    public static class ProducerCfg {
        String bootstrapServers;
        String transactionalId;
    }

    /**
     * Consumer client configuration relevant to the ledger.
     */
    @Value
    public static class ConsumerCfg {
        String bootstrapServers;
        String groupId;
    }

    /**
     * Extracts {@code bootstrap.servers} and {@code transactional.id} from the
     * configuration of one {@code KafkaProducer} constructor call.
     *
     * <p>The public {@code KafkaProducer} constructors accept only a {@code Map} or
     * {@code Properties} (plus optional serializers), never a {@code ProducerConfig}
     * instance, so a constructor argument either is one of those two shapes or does
     * not carry client configuration at all.
     */
    public static ProducerCfg parseProducerConfig(Object[] args) {
        String bootstrapServers = null;
        String transactionalId = null;
        for (Object arg : args) {
            if (arg instanceof Map<?, ?> map) {
                bootstrapServers = BootstrapUtil.asString(map.get("bootstrap.servers"));
                transactionalId = (String) map.get("transactional.id");
            }
            if (bootstrapServers != null) {
                break;
            }
        }
        return new ProducerCfg(bootstrapServers, transactionalId);
    }

    /**
     * Extracts {@code bootstrap.servers} and {@code group.id} from the configuration
     * of one {@code KafkaConsumer} constructor call.
     *
     * <p>The public {@code KafkaConsumer} constructors accept only a {@code Map} or
     * {@code Properties} (plus optional deserializers), never a {@code ConsumerConfig}
     * instance, so a constructor argument either is one of those two shapes or does
     * not carry client configuration at all.
     */
    public static ConsumerCfg parseConsumerConfig(Object[] args) {
        String bootstrapServers = null;
        String groupId = null;
        for (Object arg : args) {
            if (arg instanceof Map<?, ?> map) {
                bootstrapServers = BootstrapUtil.asString(map.get("bootstrap.servers"));
                groupId = (String) map.get("group.id");
            }
            if (bootstrapServers != null) {
                break;
            }
        }
        return new ConsumerCfg(bootstrapServers, groupId);
    }
}
