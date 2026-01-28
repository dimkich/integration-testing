package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class PendingTasks2 {
    public byte taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
