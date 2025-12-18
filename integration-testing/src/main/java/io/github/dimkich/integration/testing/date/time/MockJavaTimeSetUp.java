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

/**
 * Sets up ByteBuddy instrumentation to mock Java time-related functionality for testing purposes.
 * <p>
 * This class uses Java agents to intercept and replace calls to time-related methods such as
 * {@link System#currentTimeMillis()} with mockable alternatives provided by {@link JavaTimeAdvice}.
 * The instrumentation is applied to various Java time classes including {@link Date}, {@link Calendar},
 * {@link TimeZone}, {@link Clock}, and their related implementation classes.
 * </p>
 * <p>
 * The setup process involves:
 * <ul>
 *   <li>Injecting {@link JavaTimeAdvice} into the system class loader</li>
 *   <li>Configuring ByteBuddy to replace time-related method calls in core Java classes</li>
 *   <li>Optionally extending instrumentation to user-specified classes/packages via {@link MockJavaTime} annotation</li>
 * </ul>
 * </p>
 *
 * @see MockJavaTime
 * @see JavaTimeAdvice
 */
public class MockJavaTimeSetUp {
    /**
     * Flag indicating whether the ByteBuddy instrumentation has been initialized.
     * This ensures that the initialization process (injecting advice classes and setting up
     * core Java time class transformations) only happens once, even if {@link #setUp(MockJavaTime)}
     * is called multiple times.
     */
    @Getter
    private static boolean initialized = false;
    private static boolean javaTimeAdviceToSystemClassLoader = false;
    /**
     * ASM visitor wrapper that fixes a bug in the JDK which forgets reconstruction of parameter names in some builds.
     * <p>
     * This wrapper attempts to use Mockito's {@code ParameterWritingVisitorWrapper} if available,
     * otherwise falls back to a no-op visitor. The wrapper is applied during bytecode transformation
     * to preserve parameter name information that would otherwise be lost.
     * </p>
     * <p>
     * For more details, see:
     * <a href="https://github.com/raphw/byte-buddy/issues/1562">ByteBuddy issue #1562</a>
     * </p>
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

    /**
     * Moves the {@link JavaTimeAdvice} class to the system class loader so it can be accessed
     * by transformed classes that may be loaded by different class loaders.
     * <p>
     * This is necessary because ByteBuddy transformations may occur in classes loaded by various
     * class loaders, but those classes need to be able to invoke the advice methods. By injecting
     * the advice class into the boot class loader using {@link ClassInjector.UsingUnsafe},
     * it becomes accessible to all class loaders in the JVM.
     * </p>
     * <p>
     * The method uses ByteBuddy to redefine the class and then injects the resulting bytecode
     * into the boot class loader.
     * </p>
     */
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

    /**
     * Sets up Java time mocking based on the provided {@link MockJavaTime} annotation configuration.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Initializes the ByteBuddy agent if not already done (one-time setup for core Java time classes)</li>
     *   <li>If the annotation specifies additional classes/packages to mock (via {@link MockJavaTime#value()}),
     *       instruments those classes to replace {@link System#currentTimeMillis()} calls with mocked versions</li>
     * </ol>
     * </p>
     * <p>
     * The matching strategy uses "starts with" pattern matching, so specifying a package name will
     * match all classes in that package and its subpackages.
     * </p>
     *
     * @param mockJavaTime the annotation instance containing configuration for which classes/packages
     *                     should have their time methods mocked. If {@link MockJavaTime#value()} is empty,
     *                     only the core Java time classes will be instrumented.
     * @throws Exception if there is an error during agent installation or bytecode transformation
     */
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

    /**
     * Initializes the ByteBuddy agent with instrumentation for core Java time classes.
     * <p>
     * This method performs the following one-time setup:
     * <ol>
     *   <li>Moves {@link JavaTimeAdvice} to the system class loader so it can be used by transformed classes</li>
     *   <li>Sets up access to internal JDK methods that need to be wrapped (e.g., {@code VM.getNanoTimeAdjustment},
     *       {@code TimeZone.getDefaultRef})</li>
     *   <li>Configures ByteBuddy to transform the following classes:
     *     <ul>
     *       <li>Date and calendar-related classes: {@link Date}, {@link GregorianCalendar}, {@link Calendar},
     *           and various internal sun.util.calendar classes</li>
     *       <li>{@link Clock} class and all its implementations</li>
     *       <li>{@link TimeZone} class</li>
     *     </ul>
     *   </li>
     *   <li>For each transformed class, replaces calls to:
     *     <ul>
     *       <li>{@link System#currentTimeMillis()} with {@link JavaTimeAdvice#currentTimeMillis()}</li>
     *       <li>{@code VM.getNanoTimeAdjustment()} with {@link JavaTimeAdvice#getNanoTimeAdjustment(long)} (for Clock only)</li>
     *       <li>{@code TimeZone.getDefaultRef()} with {@link JavaTimeAdvice#getDefaultRef()} (for Calendar/Date/TimeZone)</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * This initialization is performed only once, as tracked by {@link #isInitialized}.
     * </p>
     *
     * @throws Exception if there is an error during reflection access, class injection, or agent installation
     */
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
