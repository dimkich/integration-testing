package io.github.dimkich.integration.testing.wait.completion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class WaitCompletionList {
    private final List<WaitCompletion> tasks;

    public void start() {
        tasks.forEach(WaitCompletion::start);
    }

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
