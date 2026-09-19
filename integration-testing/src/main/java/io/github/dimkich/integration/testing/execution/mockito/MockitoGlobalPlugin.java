package io.github.dimkich.integration.testing.execution.mockito;

import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Instrumentation plugin that installs the Mockito thread-local advice for every test,
 * keeping mock state consistent across the threads used by the framework.
 */
public class MockitoGlobalPlugin implements InstrumentationPlugin {

    @Override
    public boolean isApplicable(Class<?> testClass) {
        return true;
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) {
        return builder
                .type(named("org.mockito.internal.util.concurrent.DetachedThreadLocal"))
                .transform((b, td, cl, m, d) ->
                        b.visit(Advice.to(DetachedThreadLocalAdvice.class).on(named("get")
                                .or(named("set").or(named("clear")).or(named("pushTo")).or(named("fetchFrom"))
                                        .or(named("define")).or(named("initialValue"))))));
    }
}
