package io.github.dimkich.integration.testing.kafka;

import lombok.Value;
import org.apache.kafka.common.TopicPartition;

import java.util.Set;

/**
 * Result of a single lag check: whether unconsumed messages remain and which
 * partitions were examined.
 */
@Value
public class LagCheckResult {
    boolean hasLag;
    Set<TopicPartition> partitions;
}
