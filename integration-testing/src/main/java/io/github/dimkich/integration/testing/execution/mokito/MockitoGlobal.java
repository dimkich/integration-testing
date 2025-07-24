package io.github.dimkich.integration.testing.execution.mokito;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.implementation.Implementation;
import org.mockito.internal.util.concurrent.DetachedThreadLocal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MockitoGlobal {
    private static boolean initialized = false;

    public static void start() throws ClassNotFoundException, NoSuchMethodException {
        if (!initialized) {
            ByteBuddyAgent.install();
            Constructor<?> constructor = DetachedThreadLocal.class.getConstructor(DetachedThreadLocal.Cleaner.class);
            Method method = DetachedThreadLocalOrGlobal.class.getMethod("create", DetachedThreadLocal.Cleaner.class);
            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE))
                    .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                    .ignore(none())
                    .type(named("org.mockito.internal.creation.bytebuddy.InlineDelegateByteBuddyMockMaker"))
                    .transform((builder, td, cl, module, domain) -> builder
                            .visit(MemberSubstitution.relaxed()
                                    .constructor(is(constructor))
                                    .replaceWith(method)
                                    .on(any())))
                    .installOnByteBuddyAgent();
            initialized = true;
        }
        DetachedThreadLocalOrGlobal.setGlobal(true);
    }

    public static void stop() {
        DetachedThreadLocalOrGlobal.setGlobal(false);
    }
}
