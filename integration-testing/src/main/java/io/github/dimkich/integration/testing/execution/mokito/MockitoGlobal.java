package io.github.dimkich.integration.testing.execution.mokito;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.Implementation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * Configures Mockito's internal {@code DetachedThreadLocal} to use a single, global instance.
 * <p>
 * This class installs a ByteBuddy agent that intercepts calls to
 * {@code org.mockito.internal.util.concurrent.DetachedThreadLocal} so that
 * Mockito state can be shared across threads when needed, for example in
 * multi-threaded integration tests.
 * </p>
 * <p>
 * Use {@link #start()} to enable the global mode and {@link #stop()} to disable it.
 * The agent installation is performed only once per JVM.
 * </p>
 */
public class MockitoGlobal {
    private static boolean initialized = false;

    /**
     * Enables global mode for Mockito's {@code DetachedThreadLocal}.
     * <p>
     * On the first invocation this method installs a ByteBuddy agent that
     * redefines {@code org.mockito.internal.util.concurrent.DetachedThreadLocal}
     * and applies {@link DetachedThreadLocalAdvice} to selected methods.
     * Subsequent invocations only toggle the global flag without reinstalling the agent.
     * </p>
     *
     * @throws ClassNotFoundException if the {@code DetachedThreadLocal} class cannot be found
     * @throws NoSuchMethodException  if one of the intercepted methods cannot be resolved
     */
    public static void start() throws ClassNotFoundException, NoSuchMethodException {
        if (!initialized) {
            ByteBuddyAgent.install();
            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE))
                    .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                    .ignore(none())
                    .type(named("org.mockito.internal.util.concurrent.DetachedThreadLocal"))
                    .transform((builder, typeDescription, classLoader, module, domain) ->
                            builder.visit(Advice.to(DetachedThreadLocalAdvice.class).on(named("get")
                                    .or(named("set").or(named("clear")).or(named("pushTo")).or(named("fetchFrom"))
                                            .or(named("define")).or(named("initialValue")))))
                    )
                    .installOnByteBuddyAgent();
            initialized = true;
        }
        DetachedThreadLocalAdvice.setGlobal(true);
    }

    /**
     * Disables global mode for Mockito's {@code DetachedThreadLocal}.
     * <p>
     * This does not remove the installed agent; it only updates the
     * {@link DetachedThreadLocalAdvice} flag so that new calls no longer use
     * the global behavior.
     * </p>
     */
    public static void stop() {
        DetachedThreadLocalAdvice.setGlobal(false);
    }
}
