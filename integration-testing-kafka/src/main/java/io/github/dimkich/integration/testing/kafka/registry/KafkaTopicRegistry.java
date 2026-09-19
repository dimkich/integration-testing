package io.github.dimkich.integration.testing.kafka.registry;

import io.github.dimkich.integration.testing.kafka.config.ConnectionProperties;
import io.github.dimkich.integration.testing.kafka.config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the {@link KafkaTopicMetadata} of a topic by connection name and topic name,
 * using connection-level defaults when no topic pattern matches.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaTopicRegistry {
    private final KafkaProperties kafkaProperties;
    private final KafkaObjectFactory kafkaObjectFactory;
    private final Map<String, ConnectionTopicRegistry> connectionRegistries = new ConcurrentHashMap<>();

    /**
     * Returns the metadata of the given topic.
     *
     * @param connectionName the connection the message belongs to
     * @param topicName the topic to look up
     * @return the topic metadata, never {@code null}
     */
    public KafkaTopicMetadata getMetadata(String connectionName, String topicName) {
        return getRegistry(connectionName).getMetadata(topicName);
    }

    private ConnectionTopicRegistry getRegistry(String connectionName) {
        return connectionRegistries.computeIfAbsent(connectionName, name -> {
            log.debug("Creating topic registry for connection: [{}]", name);

            ConnectionProperties props = kafkaProperties.getConnection(name);
            KafkaTopicMetadata defaultMetadata = kafkaObjectFactory.createMetadata(props);
            ConnectionTopicRegistry registry = new ConnectionTopicRegistry(defaultMetadata);

            if (props != null && props.getTopics() != null) {
                props.getTopics().forEach((pattern, topicProps) ->
                        registry.registerTopicPattern(pattern, kafkaObjectFactory.createMetadata(topicProps)));
            }

            return registry;
        });
    }
}
