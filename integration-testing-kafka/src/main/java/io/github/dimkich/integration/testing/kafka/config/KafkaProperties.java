package io.github.dimkich.integration.testing.kafka.config;

import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka module configuration bound to the {@code integration.testing.kafka} prefix:
 * per-connection settings with property inheritance from the connection level down to
 * the topic level.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "integration.testing.kafka")
public class KafkaProperties extends TopicProperties {
    @Setter(onMethod_ = @Autowired)
    private PropertyInheritanceMerger merger;

    /**
     * Interval between lag poll iterations. Kept low so lag resolution (and
     * consumer-group stabilization) is detected promptly at the cost of more
     * frequent thread wakeups; a higher value would trade detection latency for
     * lower CPU load. Overridable via {@code integration.testing.kafka.lag-polling-interval-ms}.
     */
    private long lagPollingIntervalMs = 5;
    private long lagPollingTimeoutMs = 10000;
    private long startupStabilizationTimeoutSeconds = 30;

    /**
     * Interval between Admin API readiness checks of the expected consumer groups.
     * Kept higher than the lag polling interval so that waiting for an
     * asynchronously starting SUT does not flood the broker with
     * {@code describeConsumerGroups} calls. Overridable via
     * {@code integration.testing.kafka.readiness-polling-interval-ms}.
     */
    private long readinessPollingIntervalMs = 100;

    @Getter
    @Setter
    private Map<String, ConnectionProperties> connections;

    /**
     * Normalizes the connections map, applies property inheritance from the root
     * configuration down to the topics and validates all serializer/deserializer
     * configurations.
     */
    @PostConstruct
    public void init() {
        if (connections == null) {
            connections = new ConcurrentHashMap<>();
            return;
        }
        connections = new ConcurrentHashMap<>(connections);
        connections.forEach((name, conn) -> {
            prepare(conn);
            validate(conn, "connection[" + name + "]");
            if (conn.getTopics() != null) {
                conn.getTopics().forEach((topic, props) ->
                        validate(props, "connection[" + name + "].topics[" + topic + "]"));
            }
        });
    }

    private void validate(TopicProperties props, String path) {
        if (props.getSerializer() != null) {
            try {
                props.getSerializer().validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid serializer config at " + path + ": " + e.getMessage(), e);
            }
        }
        if (props.getDeserializer() != null) {
            try {
                props.getDeserializer().validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid deserializer config at " + path + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the configuration of the given connection, creating and preparing an
     * empty one when it is not configured explicitly.
     *
     * @param name the connection name
     * @return the connection configuration
     */
    public ConnectionProperties getConnection(String name) {
        return connections.computeIfAbsent(name, (k) -> prepare(new ConnectionProperties()));
    }

    private ConnectionProperties prepare(ConnectionProperties connection) {
        merger.merge(connection, this);
        if (connection.getTopics() != null) {
            connection.getTopics().values().forEach(topic -> merger.merge(topic, connection));
        }
        return connection;
    }
}
