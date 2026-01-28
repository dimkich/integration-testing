package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

public class PendingTasks6Child1 extends PendingTasks6 {
    @Override
    @PendingTasks6Ann
    public PendingTasks6 create() {
        return new PendingTasks6Child1();
    }
}
