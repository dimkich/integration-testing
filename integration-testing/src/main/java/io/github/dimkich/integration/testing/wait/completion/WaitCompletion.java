package io.github.dimkich.integration.testing.wait.completion;

public interface WaitCompletion {
    void start();

    boolean isAnyTaskStarted();

    void waitCompletion();
}
