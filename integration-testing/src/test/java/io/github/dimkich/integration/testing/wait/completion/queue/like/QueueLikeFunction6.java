package io.github.dimkich.integration.testing.wait.completion.queue.like;

import java.util.function.Function;

public class QueueLikeFunction6 implements Function<Object, Integer> {
    @Override
    public Integer apply(Object input) {
        return ((QueueLike6) input).taskCount();
    }
}
