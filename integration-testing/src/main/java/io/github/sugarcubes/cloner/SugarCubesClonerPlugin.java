package io.github.sugarcubes.cloner;

import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;

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
public class SugarCubesClonerPlugin implements InstrumentationPlugin {
    @Override
    public boolean isApplicable(Class<?> testClass) {
        return true;
    }

    @Override
    public void beforeInstall(Class<?> testClass, Instrumentation instrumentation) {
        if (ClonerAgent.getInstrumentation() == null) {
            ClonerAgent.agentmain("", instrumentation);
        }
    }

}
