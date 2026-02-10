package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

@QueueLike4Ann
public class QueueLike4 {
    public int taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
