package io.github.dimkich.integration.testing.wait.completion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Holds a collection of {@link WaitCompletion} tasks and provides
 * a unified API to start them and wait until all of them are finished.
 * <p>
 * The {@link #waitCompletion()} method will repeatedly check all tasks
 * until none of them report newly started work via
 * {@link WaitCompletion#isAnyTaskStarted()}.
 */
@Getter
@RequiredArgsConstructor
public class WaitCompletionList {

    /**
     * Wait-completion tasks that should be started and awaited together.
     */
    private final List<WaitCompletion> tasks;

    /**
     * Starts all configured wait-completion tasks.
     * <p>
     * This is typically called before {@link #waitCompletion()} to
     * initiate all tracked operations.
     */
    public void start() {
        tasks.forEach(WaitCompletion::start);
    }

    /**
     * Blocks until all {@link WaitCompletion} tasks have completed.
     * <p>
     * The method iteratively invokes {@link WaitCompletion#waitCompletion()}
     * on every task and then checks {@link WaitCompletion#isAnyTaskStarted()}
     * for each of them. If any task reports newly started work, the cycle
     * is repeated until no task reports new work.
     */
    public void waitCompletion() {
        boolean rerun = true;
        while (rerun) {
            rerun = false;
            for (WaitCompletion waitCompletion : tasks) {
                waitCompletion.waitCompletion();
            }
            for (WaitCompletion waitCompletion : tasks) {
                if (waitCompletion.isAnyTaskStarted()) {
                    rerun = true;
                }
            }
        }
    }
}
