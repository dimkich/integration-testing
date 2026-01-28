package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

import java.util.List;

public class PendingTasks3 {
    public List<Object> taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return List.of();
    }
}
