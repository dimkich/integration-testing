package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class QueueLike6 {
    @QueueLike6Ann
    public QueueLike6 create() {
        return new QueueLike6();
    }

    public int taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
