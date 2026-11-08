package io.github.dimkich.integration.testing.redis.replication.handler.stream.stream;

import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.model.RedisStreamEntry;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Entry-level operations shared by stream command handlers.
 * <p>
 * {@code XDEL} and {@code XDELEX} differ only in the command syntax; both drop entries by id, so
 * the handlers delegate here instead of duplicating the removal logic.
 */
final class StreamEntries {

    private StreamEntries() {
    }

    /**
     * Removes the given stream entries from the in-memory state.
     * <p>
     * Stops scanning as soon as every requested id has been removed, which keeps small deletes
     * on long streams cheap.
     *
     * @param store in-memory store to mutate
     * @param key   raw Redis key bytes
     * @param ids   stream entry ids to delete
     */
    static void removeByIds(RedisInMemoryStore store, byte[] key, byte[][] ids) {
        store.compute(key, RedisStream.class, (schema, stream) -> {
            Set<String> idsToDelete = new HashSet<>(ids.length * 2);
            for (byte[] id : ids) {
                idsToDelete.add(new String(id, StandardCharsets.UTF_8));
            }
            int remaining = idsToDelete.size();
            Iterator<RedisStreamEntry> iterator = stream.iterator();
            while (remaining > 0 && iterator.hasNext()) {
                if (idsToDelete.remove(iterator.next().getId())) {
                    iterator.remove();
                    remaining--;
                }
            }
        });
    }
}
