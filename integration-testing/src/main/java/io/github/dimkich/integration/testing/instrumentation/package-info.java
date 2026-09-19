/**
 * Byte Buddy instrumentation layer: discovers
 * {@link io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin}
 * implementations and manages the lifecycle of class transformations for a test run.
 *
 * <h2>How a plugin is activated</h2>
 * <ol>
 *   <li>Implement
 *       {@link io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin}.</li>
 *   <li>Register the implementation in {@code META-INF/services/}
 *       {@code io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin}.</li>
 *   <li>Return {@code true} from
 *       {@link io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin#isApplicable(Class)}
 *       for the test classes that need it.</li>
 * </ol>
 *
 * <p>{@link io.github.dimkich.integration.testing.instrumentation.InstrumentationManager} loads the plugins via
 * {@link java.util.ServiceLoader}, lets every active plugin configure the shared Byte Buddy
 * agent builder and installs the resulting transformations before the Spring context is
 * created. Once the context is ready, plugins are notified through {@code PluginSpringIntegrator}.</p>
 */
package io.github.dimkich.integration.testing.instrumentation;
