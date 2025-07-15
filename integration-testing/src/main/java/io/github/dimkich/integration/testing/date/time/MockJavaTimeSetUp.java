package io.github.dimkich.integration.testing.date.time;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MockJavaTimeSetUp {
    private static boolean isInitialized = false;

    public static void setUp(MockJavaTime mockJavaTime) throws Exception {
        if (!isInitialized) {
            initialize();
            isInitialized = true;
        }
        if (mockJavaTime.value().length == 0) {
            return;
        }
        ElementMatcher.Junction<NamedElement> matcher = none();
        for (String name : mockJavaTime.value()) {
            matcher = matcher.or(nameStartsWith(name));
        }

        Method currentTimeMillis = JavaTimeAdvice.class.getMethod("currentTimeMillis");
        new AgentBuilder.Default()
                .with(new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE))
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .ignore(none())
                .type(matcher)
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(named("currentTimeMillis"))
                                .replaceWith(currentTimeMillis)
                                .on(any())))
                .installOnByteBuddyAgent();
    }

    private static void initialize() throws Exception {
        moveJavaTimeAdviceToSystemClassLoader();

        Method getNanoTimeAdjustment = makeMethodAccessible(Class.forName("jdk.internal.misc.VM"),
                t -> t.getMethod("getNanoTimeAdjustment", long.class));
        JavaTimeAdvice.setRealGetNanoTimeAdjustment(SneakyFunction
                .sneaky(o -> (Long) getNanoTimeAdjustment.invoke(null, o)));

        Method getDefaultRef = makeMethodAccessible(TimeZone.class, t -> t.getDeclaredMethod("getDefaultRef"));
        JavaTimeAdvice.setRealGetDefaultRef(SneakySupplier.sneaky(() -> (TimeZone) getDefaultRef.invoke(null)));

        Method newCurrentTimeMillis = JavaTimeAdvice.class.getMethod("currentTimeMillis");
        Method newGetNanoTimeAdjustment = JavaTimeAdvice.class.getMethod("getNanoTimeAdjustment", long.class);
        Method newGetDefaultRef = JavaTimeAdvice.class.getMethod("getDefaultRef");

        Clock.class.getNestMembers(); // Inner classes of Clock is not instrumented without this line
        String[] classes = new String[]{Date.class.getName(), GregorianCalendar.class.getName(),
                "java.util.JapaneseImperialCalendar", "sun.util.calendar.AbstractCalendar",
                "sun.util.calendar.Gregorian", "sun.util.calendar.JulianCalendar",
                "sun.util.calendar.ZoneInfo", "sun.util.calendar.ZoneInfoFile"};
        new AgentBuilder.Default()
                .with(new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE))
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .disableClassFormatChanges()
                .ignore(none())
                .type(namedOneOf(classes).or(nameStartsWith(Clock.class.getName())))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(named("currentTimeMillis"))
                                .replaceWith(newCurrentTimeMillis)
                                .on(any())))
                .type(named(Clock.class.getName()))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(named("getNanoTimeAdjustment"))
                                .replaceWith(newGetNanoTimeAdjustment)
                                .on(any())))
                .type(namedOneOf(Calendar.class.getName(), Date.class.getName(), GregorianCalendar.class.getName(),
                        TimeZone.class.getName()))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(named("getDefaultRef"))
                                .replaceWith(newGetDefaultRef)
                                .on(any())))
                .installOnByteBuddyAgent();
    }

    private static void moveJavaTimeAdviceToSystemClassLoader() {
        TypePool typePool = TypePool.Default.ofSystemLoader();
        Map<TypeDescription, byte[]> types = new ByteBuddy()
                .redefine(
                        typePool.describe("io.github.dimkich.integration.testing.date.time.JavaTimeAdvice").resolve(),
                        ClassFileLocator.ForClassLoader.ofSystemLoader())
                .make()
                .getAllTypes();
        ClassInjector.UsingUnsafe.ofBootLoader().inject(types);
    }

    private static Method makeMethodAccessible(Class<?> type, SneakyFunction<Class<?>, Method, Exception> getMethod)
            throws Exception {
        Module clonerModule = MockJavaTimeSetUp.class.getModule();
        String packageName = type.getPackageName();
        if (!type.getModule().isOpen(packageName, clonerModule)) {
            ByteBuddyAgent.install().redefineModule(type.getModule(), Set.of(), Map.of(),
                    Map.of(packageName, Set.of(clonerModule)), Set.of(), Map.of());
        }
        Method getNanoTimeAdjustment = getMethod.apply(type);
        getNanoTimeAdjustment.setAccessible(true);
        return getNanoTimeAdjustment;
    }
}
