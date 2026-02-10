package io.github.dimkich.integration.testing;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

/**
 * Manages the lifecycle of Byte Buddy instrumentation within the application.
 * <p>
 * This manager is responsible for:
 * <ul>
 *     <li>Creating and configuring the base {@link AgentBuilder} with sensible defaults.</li>
 *     <li>Tracking transformed classes to allow surgical re-transformation during reset.</li>
 *     <li>Preventing redundant instrumentation installs if no transformations were defined.</li>
 *     <li>Restoring the JVM state by removing applied transformers and reverting class changes.</li>
 * </ul>
 * <p>
 * The manager uses a reference-equality check in {@link #install(AgentBuilder, Instrumentation)}
 * to determine if any custom transformations were added to the base builder.
 */
public class InstrumentationManager {
    private final Set<Class<?>> transformedClasses = ConcurrentHashMap.newKeySet();
    private ResettableClassFileTransformer transformer;
    private AgentBuilder agentBuilder;

    /**
     * Creates a pre-configured {@link AgentBuilder} with re-transformation strategy enabled.
     * <p>
     * The builder is configured to:
     * <ul>
     *     <li>Use {@link AgentBuilder.RedefinitionStrategy#RETRANSFORMATION} for applying changes.</li>
     *     <li>Disable Byte Buddy's internal context to minimize memory footprint.</li>
     *     <li>Register a listener that tracks every transformed class for future cleanup.</li>
     *     <li>Ignore internal Byte Buddy and JDK proxy classes by default.</li>
     * </ul>
     *
     * @return a new instance of {@link AgentBuilder} configured for this manager.
     */
    public AgentBuilder createAgentBuilder() {
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
     * Installs the provided builder onto the JVM.
     * <p>
     * Optimization: If the {@code finalBuilder} is the same instance as the one returned
     * by {@link #createAgentBuilder()}, the installation is skipped as no transformations
     * were defined.
     *
     * @param builder         the builder containing the instrumentation logic.
     * @param instrumentation the JVM instrumentation instance.
     */
    public void install(AgentBuilder builder, Instrumentation instrumentation) {
        if (builder == agentBuilder) {
            return;
        }
        this.transformer = builder.installOn(instrumentation);
    }

    /**
     * Removes the installed transformer and reverts all changes made to the classes.
     * <p>
     * If transformations were applied, this method performs a targeted reset only on
     * the classes tracked during the agent's lifetime to ensure high performance
     * and stability.
     *
     * @param instrumentation the JVM instrumentation instance.
     */
    public void reset(Instrumentation instrumentation) {
        if (transformer == null) {
            return;
        }
        try {
            if (!transformedClasses.isEmpty()) {
                transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                        new AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Explicit(transformedClasses));
            } else {
                transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
            }
        } finally {
            this.transformer = null;
            this.transformedClasses.clear();
            this.agentBuilder = null;
        }
    }
}
