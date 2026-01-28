package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import io.github.dimkich.integration.testing.wait.completion.MethodCalls;

public class PendingTasks5 {
    public static PendingTasks5 create() {
        return new PendingTasks5();
    }

    public int taskCount() {
        MethodCalls.add(getClass(), "taskCount");
        return 0;
    }
}
