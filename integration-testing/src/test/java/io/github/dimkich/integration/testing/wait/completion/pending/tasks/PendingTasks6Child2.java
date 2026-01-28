package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

public class PendingTasks6Child2 extends PendingTasks6 {
    @Override
    public PendingTasks6 create() {
        return new PendingTasks6Child2();
    }
}
