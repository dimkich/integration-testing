package io.github.dimkich.integration.testing.config;

import io.github.dimkich.integration.testing.instrumentation.InstrumentationManager;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Configuration;

/**
 * Bridges instrumentation plugins and the Spring context: once all singletons are
 * instantiated, notifies every active {@link InstrumentationPlugin} through
 * {@link InstrumentationPlugin#onSpringContextReady(BeanFactory)}.
 *
 * <p>This is required because plugins are created outside of Spring (via
 * {@link InstrumentationManager} and {@link java.util.ServiceLoader}) but may need
 * access to beans, for example to read test infrastructure state.</p>
 */
@Configuration
@RequiredArgsConstructor
public class PluginSpringIntegrator implements SmartInitializingSingleton {

    private final BeanFactory beanFactory;

    @Override
    public void afterSingletonsInstantiated() {
        for (InstrumentationPlugin plugin : InstrumentationManager.getActivePlugins()) {
            try {
                plugin.onSpringContextReady(beanFactory);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke onSpringContextReady on " + plugin.getClass().getName(), e);
            }
        }
    }
}
