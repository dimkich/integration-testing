package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class PendingTasks6 {
    @PendingTasks6Ann
    public PendingTasks6 create() {
        return new PendingTasks6();
    }

    public int taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
