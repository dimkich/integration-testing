package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

@PendingTasks4Ann
public class PendingTasks4 {
    public int taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
