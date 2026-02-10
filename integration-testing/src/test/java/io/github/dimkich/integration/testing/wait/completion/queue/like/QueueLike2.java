package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class QueueLike2 {
    public byte taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
