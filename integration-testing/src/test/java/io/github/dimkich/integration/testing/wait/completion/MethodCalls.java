package io.github.dimkich.integration.testing.wait.completion;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class MethodCalls {
    @Getter
    private static final List<String> methods = new ArrayList<>();

    public static void reset() {
        methods.clear();
    }

    public static void add(Class<?> cls, String methodName) {
        methods.add(cls.getSimpleName() + "#" + methodName);
    }
}
