package io.github.dimkich.integration.testing.date.time;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import lombok.Getter;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MockJavaTimeSetUp {
    @Getter
    private static boolean initialized = false;
    private static boolean javaTimeAdviceToSystemClassLoader = false;
    /**
     * Fix a bug in the JDK which forgets reconstruction of parameter names in some builds.
     * <a href="https://github.com/raphw/byte-buddy/issues/1562">method lost parameter names</a>
     */
    private static final Function<TypeDescription, AsmVisitorWrapper> parameterWritingVisitorWrapper;

    static {
        Constructor<?> constructor;
        try {
            Class<?> cls = Class.forName("org.mockito.internal.creation.bytebuddy.InlineBytecodeGenerator$ParameterWritingVisitorWrapper");
            constructor = cls.getDeclaredConstructor(Class.class);
            constructor.setAccessible(true);
        } catch (Exception e) {
            constructor = null;
        }
        Constructor<?> c = constructor;
        parameterWritingVisitorWrapper = name -> {
            try {
                if (c != null && name instanceof TypeDescription.ForLoadedType) {
                    return (AsmVisitorWrapper.AbstractBase) c.newInstance(Class.forName(name.getName()));
                }
            } catch (ReflectiveOperationException ignore) {
            }
            return AsmVisitorWrapper.NoOp.INSTANCE;
        };
    }

    public static void moveJavaTimeAdviceToSystemClassLoader() {
        if (javaTimeAdviceToSystemClassLoader) {
            return;
        }
        TypePool typePool = TypePool.Default.ofSystemLoader();
        Map<TypeDescription, byte[]> types = new ByteBuddy()
                .redefine(
                        typePool.describe("io.github.dimkich.integration.testing.date.time.JavaTimeAdvice").resolve(),
                        ClassFileLocator.ForClassLoader.ofSystemLoader())
                .make()
                .getAllTypes();
        ClassInjector.UsingUnsafe.ofBootLoader().inject(types);
        javaTimeAdviceToSystemClassLoader = true;
    }

    public static void setUp(MockJavaTime mockJavaTime) throws Exception {
        if (!initialized) {
            initialize();
            initialized = true;
        }
        if (mockJavaTime.value().length == 0) {
            return;
        }
        ElementMatcher.Junction<NamedElement> matcher = none();
        for (String name : mockJavaTime.value()) {
            matcher = matcher.or(nameStartsWith(name));
        }

        Method currentTimeMillis = System.class.getMethod("currentTimeMillis");
        Method newCurrentTimeMillis = JavaTimeAdvice.class.getMethod("currentTimeMillis");
        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE))
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .ignore(none())
                .type(matcher)
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(is(currentTimeMillis))
                                .replaceWith(newCurrentTimeMillis)
                                .on(any()))
                        .visit(parameterWritingVisitorWrapper.apply(td)))
                .installOnByteBuddyAgent();
    }

    public static void shutDown() {
        if (initialized) {
            JavaTimeAdvice.setCallRealMethod(null);
            JavaTimeAdvice.setCurrentTimeMillis(null);
            JavaTimeAdvice.setGetNanoTimeAdjustment(null);
            JavaTimeAdvice.setGetDefaultRef(null);
            initialized = false;
        }
    }

    private static void initialize() throws Exception {
        Method getNanoTimeAdjustment = ByteBuddyUtils.makeAccessible(Class.forName("jdk.internal.misc.VM")
                .getDeclaredMethod("getNanoTimeAdjustment", long.class));
        JavaTimeAdvice.setRealGetNanoTimeAdjustment(SneakyFunction
                .sneaky(o -> (Long) getNanoTimeAdjustment.invoke(null, o)));

        Method getDefaultRef = ByteBuddyUtils.makeAccessible(TimeZone.class.getDeclaredMethod("getDefaultRef"));
        JavaTimeAdvice.setRealGetDefaultRef(SneakySupplier.sneaky(() -> (TimeZone) getDefaultRef.invoke(null)));
        Method currentTimeMillis = System.class.getMethod("currentTimeMillis");

        Method newCurrentTimeMillis = JavaTimeAdvice.class.getMethod("currentTimeMillis");
        Method newGetNanoTimeAdjustment = JavaTimeAdvice.class.getMethod("getNanoTimeAdjustment", long.class);
        Method newGetDefaultRef = JavaTimeAdvice.class.getMethod("getDefaultRef");

        String[] classes = new String[]{Date.class.getName(), GregorianCalendar.class.getName(),
                "java.util.JapaneseImperialCalendar", "sun.util.calendar.AbstractCalendar",
                "sun.util.calendar.Gregorian", "sun.util.calendar.JulianCalendar",
                "sun.util.calendar.ZoneInfo", "sun.util.calendar.ZoneInfoFile"};
        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE))
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .ignore(none())
                .type(namedOneOf(classes).or(nameStartsWith(Clock.class.getName())))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(is(currentTimeMillis))
                                .replaceWith(newCurrentTimeMillis)
                                .on(any()))
                        .visit(parameterWritingVisitorWrapper.apply(td)))
                .type(named(Clock.class.getName()))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(is(getNanoTimeAdjustment))
                                .replaceWith(newGetNanoTimeAdjustment)
                                .on(any()))
                        .visit(parameterWritingVisitorWrapper.apply(td)))
                .type(namedOneOf(Calendar.class.getName(), Date.class.getName(), GregorianCalendar.class.getName(),
                        TimeZone.class.getName()))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(is(getDefaultRef))
                                .replaceWith(newGetDefaultRef)
                                .on(any()))
                        .visit(parameterWritingVisitorWrapper.apply(td)))
                .installOnByteBuddyAgent();
    }
}
