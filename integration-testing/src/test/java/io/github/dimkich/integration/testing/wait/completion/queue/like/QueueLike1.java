package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class QueueLike1 {
    public int taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
