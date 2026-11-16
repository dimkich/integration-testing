package io.github.dimkich.integration.testing.redis.diff;

import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;

import java.util.Map;

/**
 * Describes a Redis value with its TTL as an attribute and its payload as the body.
 * <p>
 * The engine diffs the payload by the same rules as any other value, so a changed value contains
 * only changed fields and a value that appears is written whole. A string value becomes the text
 * of the element, a hash, list, set, sorted set or stream keeps its own structure. The
 * {@code type} attribute comes from the payload type, so a string has no type while a hash is
 * written as {@code RedisHash}.
 */
public class RedisEntryPresenter implements DiffPresenter<RedisEntry> {

    @Override
    public Object present(RedisEntry entry, DiffAttributes.Builder attributes) {
        attributes.addAttributes(ttl(entry));
        return entry.getData();
    }

    @Override
    public String type(RedisEntry entry, DiffValue body, TypeNames types) {
        return types.typeOf(entry.getData());
    }

    private Map<String, String> ttl(RedisEntry entry) {
        if (entry.getTtl() == null) {
            return Map.of();
        }
        return Map.of("ttl", entry.getTtl().toString());
    }
}
