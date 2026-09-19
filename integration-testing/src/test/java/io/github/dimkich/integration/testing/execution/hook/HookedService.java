package io.github.dimkich.integration.testing.execution.hook;

public class HookedService {
    private final String name;

    public HookedService(String name) {
        this.name = name;
    }

    public String process(String value) {
        return name + ":" + value;
    }

    public String conditional(String value) {
        return name + "?" + value;
    }

    public String fail(String value) {
        throw new IllegalStateException("boom:" + value);
    }

    public static String staticProcess(String value) {
        return "static:" + value;
    }
}
