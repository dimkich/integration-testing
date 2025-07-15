package io.github.sugarcubes.cloner;

import java.lang.instrument.Instrumentation;

/**
 * Passing {@link Instrumentation} to sugarcubes.cloner. It allows clone everything(using reflection) without
 * worry about error "module is not opened"
 */
public class ClonerAgentSetUp {
    public static void setClonerInstrumentationIfNone(Instrumentation instrumentation) {
        if (ClonerAgent.getInstrumentation() == null) {
            ClonerAgent.agentmain("", instrumentation);
        }
    }
}
