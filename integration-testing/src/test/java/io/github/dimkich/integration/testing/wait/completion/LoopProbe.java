package io.github.dimkich.integration.testing.wait.completion;

import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Synthetic {@link WaitCompletion} used by {@code waitCompletion.xml} to observe the
 * stabilization loop of {@link WaitCompletionList}.
 * <p>
 * The probe reports activity for the first {@link #setRounds(int)} steps and rest after that,
 * so the XML test can assert that the coordinator really ran several steps and stopped only
 * when every task returned {@code false}.
 */
public class LoopProbe implements WaitCompletion {
    private final AtomicInteger steps = new AtomicInteger();
    @Setter
    private volatile int rounds;

    public int getSteps() {
        return steps.get();
    }

    @Override
    public void start() {
        steps.set(0);
    }

    @Override
    public boolean awaitAndCheckActivity() {
        return steps.incrementAndGet() <= rounds;
    }
}
