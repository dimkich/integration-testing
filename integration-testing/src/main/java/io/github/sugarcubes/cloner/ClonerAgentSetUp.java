package io.github.sugarcubes.cloner;

import java.lang.instrument.Instrumentation;

/**
 * Utility class for configuring the SugarCubes {@link ClonerAgent} at runtime.
 * <p>
 * The cloner library uses the JVM {@link Instrumentation} API to bypass Java
 * module boundaries when performing deep reflection-based cloning. Without
 * instrumentation, cloning objects from non-opened modules may fail with
 * "module is not opened" errors.
 * </p>
 * <p>
 * This class provides a single entry point which can be called from application
 * or test bootstrap code to supply the {@link Instrumentation} instance that
 * was obtained from a Java agent.
 * </p>
 */
public class ClonerAgentSetUp {

    /**
     * Registers the given {@link Instrumentation} instance with {@link ClonerAgent}
     * if and only if no instrumentation has been set yet.
     * <p>
     * This method is idempotent with respect to already-initialized instrumentation:
     * if {@link ClonerAgent#getInstrumentation()} returns a non-{@code null} value,
     * the call is ignored and the passed {@code instrumentation} is not used.
     * Otherwise, {@link ClonerAgent#agentmain(String, Instrumentation)} is invoked
     * to initialize the cloner with the provided instance.
     * </p>
     *
     * @param instrumentation the {@link Instrumentation} instance obtained from a
     *                        Java agent; must not be {@code null} when first called
     */
    public static void setClonerInstrumentationIfNone(Instrumentation instrumentation) {
        if (ClonerAgent.getInstrumentation() == null) {
            ClonerAgent.agentmain("", instrumentation);
        }
    }
}
