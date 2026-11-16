package io.github.dimkich.integration.testing.redis.diff;

import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;

import java.math.BigDecimal;

/**
 * Describes one member of a Redis sorted set: the only data of a member is its score, so the
 * element carries an attribute and its body is empty.
 * <p>
 * A member is identified by its own name, which is the difference key, so the rendering of the
 * element is just {@code <member score="10.5"/>}.
 */
public class RedisZSetEntryPresenter implements DiffPresenter<RedisZSetEntry> {

    @Override
    public Object present(RedisZSetEntry entry, DiffAttributes.Builder attributes) {
        BigDecimal score = entry.getScore();
        if (score != null) {
            attributes.addAttribute("score", score.stripTrailingZeros().toPlainString());
        }
        return null;
    }

    /**
     * The element has no type of its own: the member is its key and the score is its only data,
     * so the default value type would only add noise.
     */
    @Override
    public String type(RedisZSetEntry entry, DiffValue body, TypeNames types) {
        return null;
    }
}
