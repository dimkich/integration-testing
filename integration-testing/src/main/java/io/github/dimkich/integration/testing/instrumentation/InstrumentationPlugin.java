package io.github.dimkich.integration.testing.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import org.springframework.beans.factory.BeanFactory;

import java.lang.instrument.Instrumentation;

/**
 * Service provider interface for Byte Buddy instrumentation plugins.
 *
 * <p>Implementations are discovered through {@link java.util.ServiceLoader} (register the
 * class in {@code META-INF/services}) and become active for a test class when
 * {@link #isApplicable(Class)} returns {@code true}. The lifecycle is orchestrated by
 * {@link InstrumentationManager} in the following order:</p>
 * <ol>
 *   <li>{@link #beforeInstall(Class, Instrumentation)}</li>
 *   <li>{@link #configureBuilder(Class, AgentBuilder)} for every active plugin</li>
 *   <li>installation of the resulting {@link AgentBuilder}</li>
 *   <li>{@link #afterInstall(Class, Instrumentation)}</li>
 *   <li>{@link #onSpringContextReady(BeanFactory)} once the Spring context is ready</li>
 *   <li>{@link #cleanup()} after the test class has finished</li>
 * </ol>
 */
public interface InstrumentationPlugin {
    /**
     * Decides whether this plugin should be active for the given test class.
     *
     * @param testClass the test class being prepared
     * @return {@code true} if the plugin should participate in instrumentation
     */
    boolean isApplicable(Class<?> testClass);

    /**
     * Called before the Byte Buddy agent is installed, for state that must be
     * prepared prior to any transformation.
     *
     * @param testClass the test class being prepared
     * @param instrumentation the JVM instrumentation instance
     */
    default void beforeInstall(Class<?> testClass, Instrumentation instrumentation) {
    }

    /**
     * Contributes type transformations to the shared {@link AgentBuilder}.
     *
     * @param testClass the test class being prepared
     * @param builder the shared builder to extend
     * @return the builder to install; either the given instance or a new one
     * @throws Exception if the builder cannot be configured
     */
    default AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) throws Exception {
        return builder;
    }

    /**
     * Called after the agent has been installed, for state that depends on the
     * installed transformations.
     *
     * @param testClass the test class being prepared
     * @param instrumentation the JVM instrumentation instance
     * @throws Exception if post-installation logic fails
     */
    default void afterInstall(Class<?> testClass, Instrumentation instrumentation) throws Exception {
    }

    /**
     * Called once the Spring application context is fully initialized, allowing the
     * plugin to connect instrumentation to Spring beans.
     *
     * @param beanFactory the bean factory of the ready application context
     */
    default void onSpringContextReady(BeanFactory beanFactory) {
    }

    /**
     * Releases resources and instrumentation state after the test class has finished.
     *
     * @throws Exception if cleanup fails
     */
    default void cleanup() throws Exception {
    }
}
