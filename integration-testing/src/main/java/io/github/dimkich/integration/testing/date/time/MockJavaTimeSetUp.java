package io.github.dimkich.integration.testing.date.time;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import lombok.Getter;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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
                        .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(td)))
                .installOnByteBuddyAgent();
    }

    /**
     * Resets Java time mocking to its default behavior.
     * <p>
     * This method clears all delegates in {@link JavaTimeAdvice} and marks this setup as
     * uninitialized so that {@link #setUp(MockJavaTime)} can perform initialization again on
     * the next invocation.
     * </p>
     */
    public static void tearDown() {
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
     * This initialization is performed only once, as tracked by {@link #isInitialized()}.
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
                        .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(td)))
                .type(named(Clock.class.getName()))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(is(getNanoTimeAdjustment))
                                .replaceWith(newGetNanoTimeAdjustment)
                                .on(any()))
                        .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(td)))
                .type(namedOneOf(Calendar.class.getName(), Date.class.getName(), GregorianCalendar.class.getName(),
                        TimeZone.class.getName()))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(MemberSubstitution.relaxed()
                                .method(is(getDefaultRef))
                                .replaceWith(newGetDefaultRef)
                                .on(any()))
                        .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(td)))
                .installOnByteBuddyAgent();
    }
}
