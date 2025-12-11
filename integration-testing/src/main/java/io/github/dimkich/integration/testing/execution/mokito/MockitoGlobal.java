package io.github.dimkich.integration.testing.execution.mokito;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.Implementation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

public class MockitoGlobal {
    private static boolean initialized = false;

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

    public static void stop() {
        DetachedThreadLocalAdvice.setGlobal(false);
    }
}
