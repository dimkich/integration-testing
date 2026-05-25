package io.github.dimkich.integration.testing.redis.registry;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that maps Redis key patterns to {@link RedisDataSchemaMetadata} using longest-prefix
 * matching.
 * <p>
 * Key patterns are stored in a trie; for a given Redis key, the schema corresponding to the
 * longest matching pattern prefix is returned. When no pattern matches, the
 * {@link #getDefaultKeySchema() default schema} is used.
 *
 * @see RedisDataSchemaMetadata
 */
public class ConnectionSchemaRegistry {
    private final SchemaTrie trie;
    @Getter
    private final RedisDataSchemaMetadata defaultKeySchema;

    /**
     * Creates a new registry with the given key patterns and default schema.
     *
     * @param keySchemas     map of Redis key patterns (prefixes) to their schema metadata
     * @param defaultKeySchema schema to use when no pattern matches a key
     */
    public ConnectionSchemaRegistry(Map<String, RedisDataSchemaMetadata> keySchemas,
                                    RedisDataSchemaMetadata defaultKeySchema) {
        this.trie = new SchemaTrie();
        keySchemas.forEach(trie::insert);
        this.defaultKeySchema = defaultKeySchema;
    }

    /**
     * Returns the schema metadata for the longest matching pattern prefix of the given Redis key.
     * If no registered pattern matches, returns the default schema.
     *
     * @param redisKey the Redis key to look up
     * @return the matching schema metadata, or the default schema if no pattern matches
     */
    public RedisDataSchemaMetadata findLongestMatchingSchema(String redisKey) {
        RedisDataSchemaMetadata match = trie.findLongest(redisKey);
        return (match != null) ? match : defaultKeySchema;
    }

    /**
     * Trie structure for longest-prefix matching of Redis key patterns against schema metadata.
     */
    private static class SchemaTrie {
        private final Node root = new Node();

        void insert(String pattern, RedisDataSchemaMetadata metadata) {
            Node current = root;
            for (char c : pattern.toCharArray()) {
                current = current.children.computeIfAbsent(c, k -> new Node());
            }
            current.metadata = metadata;
        }

        RedisDataSchemaMetadata findLongest(String key) {
            Node current = root;
            RedisDataSchemaMetadata lastFoundMetadata = null;

            for (char c : key.toCharArray()) {
                current = current.children.get(c);
                if (current == null) {
                    break;
                }
                if (current.metadata != null) {
                    lastFoundMetadata = current.metadata;
                }
            }
            return lastFoundMetadata;
        }

        private static class Node {
            private final Map<Character, Node> children = new HashMap<>();
            private RedisDataSchemaMetadata metadata;
        }
    }
}