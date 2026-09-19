package io.github.dimkich.integration.testing.instrumentation;

import lombok.Getter;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

/**
 * Manages the lifecycle of Byte Buddy instrumentation and plugins within the application.
 */
public class InstrumentationManager {
    private static final Set<Class<?>> transformedClasses = ConcurrentHashMap.newKeySet();
    private static ResettableClassFileTransformer transformer;
    private static AgentBuilder agentBuilder;

    private static Instrumentation instrumentation;
    @Getter
    private static final List<InstrumentationPlugin> activePlugins = new ArrayList<>();

    /**
     * Installs the ByteBuddy agent, discovers applicable plugins, and applies transformations.
     */
    public static void install(Class<?> testClass) throws Exception {
        instrumentation = ByteBuddyAgent.install();

        ServiceLoader<InstrumentationPlugin> loader = ServiceLoader.load(InstrumentationPlugin.class);
        for (InstrumentationPlugin plugin : loader) {
            if (plugin.isApplicable(testClass)) {
                activePlugins.add(plugin);
            }
        }

        for (InstrumentationPlugin plugin : activePlugins) {
            plugin.beforeInstall(testClass, instrumentation);
        }

        AgentBuilder builder = createAgentBuilder();
        for (InstrumentationPlugin plugin : activePlugins) {
            builder = plugin.configureBuilder(testClass, builder);
        }

        if (builder != agentBuilder) {
            transformer = builder.installOn(instrumentation);
        }

        for (InstrumentationPlugin plugin : activePlugins) {
            plugin.afterInstall(testClass, instrumentation);
        }
    }

    private static AgentBuilder createAgentBuilder() {
        return agentBuilder = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE))
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    @SuppressWarnings("NullableProblems")
                    public void onTransformation(TypeDescription td, ClassLoader cl, JavaModule m, boolean loaded, DynamicType dt) {
                        if (loaded) {
                            try {
                                transformedClasses.add(Class.forName(td.getName(), false, cl));
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                })
                .ignore(nameStartsWith("net.bytebuddy.")
                        .or(ElementMatchers.nameStartsWith("jdk.proxy")));
    }

    /**
     * Removes the installed transformer, reverts class modifications, and cleans up plugins.
     */
    public static void clear() {
        for (InstrumentationPlugin plugin : activePlugins) {
            try {
                plugin.cleanup();
            } catch (Exception e) {
                // Log or propagate depending on necessity
            }
        }
        activePlugins.clear();

        if (transformer != null && instrumentation != null) {
            try {
                if (!transformedClasses.isEmpty()) {
                    transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                            new AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Explicit(transformedClasses));
                } else {
                    transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
                }
            } finally {
                transformer = null;
                transformedClasses.clear();
                agentBuilder = null;
                instrumentation = null;
            }
        }
    }
}
