package io.github.dimkich.integration.testing.redis.diff;

import io.github.dimkich.integration.testing.redis.model.RedisDataComparator;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

/**
 * Describes a Redis sorted set as members with their scores.
 * <p>
 * A member identifies the element, so it is the difference key and the score is its only data:
 * the engine turns a changed score into a changed field, a new member into an {@code insert} and a
 * removed member into a {@code delete}. A whole set is written as plain fields.
 * <p>
 * Members are rendered by the common key rules, so a complex member type needs a key presenter
 * registered via {@code TestSetupModule.addDiffKeyPresenter} that returns a compact scalar body.
 * A value whose member cannot be expressed as an identifier is better presented as a collection:
 * then the member becomes the element body and the score an attribute.
 */
public class RedisZSetPresenter implements DiffPresenter<RedisZSet> {

    @Override
    public Object present(RedisZSet zSet, DiffAttributes.Builder attributes) {
        Map<Object, RedisZSetEntry> members = new TreeMap<>(new RedisDataComparator());
        for (RedisZSetEntry entry : zSet) {
            members.put(entry.getMember(), normalized(entry));
        }
        return members;
    }

    /**
     * Scores are compared numerically: {@code 20} and {@code 20.0} are the same score, so the
     * projection keeps one form of it.
     */
    private RedisZSetEntry normalized(RedisZSetEntry entry) {
        BigDecimal score = entry.getScore();
        if (score == null) {
            return entry;
        }
        return new RedisZSetEntry(entry.getMember(),
                new BigDecimal(score.stripTrailingZeros().toPlainString()));
    }
}
