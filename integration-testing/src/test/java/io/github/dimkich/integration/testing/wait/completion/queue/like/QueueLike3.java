package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

import java.util.List;

public class QueueLike3 {
    public List<Object> taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return List.of();
    }
}
