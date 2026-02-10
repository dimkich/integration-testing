package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class QueueLike5 {
    public static QueueLike5 create() {
        return new QueueLike5();
    }

    public int taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
