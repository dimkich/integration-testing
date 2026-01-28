package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import eu.ciechanowiec.sneakyfun.SneakyFunction;

public class PendingTasksFunction6 implements SneakyFunction<Object, Integer, Exception> {
    @Override
    public Integer apply(Object input) {
        return ((PendingTasks6) input).taskCount();
    }
}
