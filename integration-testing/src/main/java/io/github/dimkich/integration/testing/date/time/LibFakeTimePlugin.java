package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Instrumentation plugin for tests that use libfaketime inside Docker containers
 * ({@link MockJavaTime#dockerImages()}). It adjusts container startup so that the
 * faketime library is applied within the container.
 */
public class LibFakeTimePlugin implements InstrumentationPlugin {

    @Override
    public boolean isApplicable(Class<?> testClass) {
        MockJavaTime mockJavaTime = testClass.getAnnotation(MockJavaTime.class);
        return mockJavaTime != null && mockJavaTime.dockerImages().length > 0;
    }

    @Override
    public void beforeInstall(Class<?> testClass, java.lang.instrument.Instrumentation instrumentation) {
        MockJavaTime mockJavaTime = testClass.getAnnotation(MockJavaTime.class);
        LibFakeTimeTracker.setUp(mockJavaTime.dockerImages());
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) {
        return builder
                .type(ElementMatchers.hasSuperType(ElementMatchers.named("org.testcontainers.containers.GenericContainer")))
                .transform((b, td, cl, module, domain) -> b
                        .visit(Advice.to(LibFakeTimeAdvice.class)
                                .on(named("start")
                                        .and(takesArguments(0))
                                        .and(isPublic()))));
    }

    @Override
    public void cleanup() throws Exception {
        LibFakeTimeTracker.tearDown();
    }
}
