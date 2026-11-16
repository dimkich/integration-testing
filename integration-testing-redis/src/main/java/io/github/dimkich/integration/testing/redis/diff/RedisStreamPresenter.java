package io.github.dimkich.integration.testing.redis.diff;

import io.github.dimkich.integration.testing.redis.model.RedisDataComparator;
import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.model.RedisStreamEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;

import java.util.Map;
import java.util.TreeMap;

/**
 * Describes a Redis stream as entries identified by their id.
 * <p>
 * The id identifies the entry, so it is the difference key and the fields are its body. The engine
 * turns a new entry into an {@code insert} and a removed one into a {@code delete}; a whole
 * stream is written as plain data.
 */
public class RedisStreamPresenter implements DiffPresenter<RedisStream> {

    @Override
    public Object present(RedisStream stream, DiffAttributes.Builder attributes) {
        Map<Object, Map<Object, Object>> entries = new TreeMap<>(new RedisDataComparator());
        for (RedisStreamEntry entry : stream) {
            entries.put(entry.getId(), entry.getFields());
        }
        return entries;
    }
}
