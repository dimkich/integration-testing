package io.github.dimkich.integration.testing.date.time;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.execution.MockAnswer;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import lombok.Getter;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Instrumentation plugin that replaces the system clock with the date and time
 * configured by the test when the test class is annotated with {@link MockJavaTime}.
 */
public class MockJavaTimePlugin implements InstrumentationPlugin {

    @Getter
    private static boolean initialized = false;
    private static Method realGetNanoTimeAdjustmentMethod;
    private static Method realGetDefaultRefMethod;

    @Override
    public boolean isApplicable(Class<?> testClass) {
        return testClass.getAnnotation(MockJavaTime.class) != null;
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) throws Exception {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        MockJavaTime mockJavaTime = testClass.getAnnotation(MockJavaTime.class);

        Method currentTimeMillis = System.class.getMethod("currentTimeMillis");
        Method newCurrentTimeMillis = JavaTimeAdvice.class.getMethod("currentTimeMillis");
        Method newGetNanoTimeAdjustment = JavaTimeAdvice.class.getMethod("getNanoTimeAdjustment", long.class);
        Method newGetDefaultRef = JavaTimeAdvice.class.getMethod("getDefaultRef");

        String[] coreClasses = new String[]{Date.class.getName(), GregorianCalendar.class.getName(),
                "java.util.JapaneseImperialCalendar", "sun.util.calendar.AbstractCalendar",
                "sun.util.calendar.Gregorian", "sun.util.calendar.JulianCalendar",
                "sun.util.calendar.ZoneInfo", "sun.util.calendar.ZoneInfoFile"};

        builder = builder
                .type(namedOneOf(coreClasses).or(nameStartsWith(Clock.class.getName())))
                .transform((b, td, cl, module, domain) -> b
                        .visit(MemberSubstitution.relaxed()
                                .method(is(currentTimeMillis))
                                .replaceWith(newCurrentTimeMillis)
                                .on(any()))
                        .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(td)))
                .type(named(Clock.class.getName()))
                .transform((b, td, cl, module, domain) -> b
                        .visit(MemberSubstitution.relaxed()
                                .method(is(realGetNanoTimeAdjustmentMethod))
                                .replaceWith(newGetNanoTimeAdjustment)
                                .on(any()))
                        .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(td)))
                .type(namedOneOf(Calendar.class.getName(), Date.class.getName(), GregorianCalendar.class.getName(),
                        TimeZone.class.getName()))
                .transform((b, td, cl, module, domain) -> b
                        .visit(MemberSubstitution.relaxed()
                                .method(is(realGetDefaultRefMethod))
                                .replaceWith(newGetDefaultRef)
                                .on(any()))
                        .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(td)));

        if (mockJavaTime.value().length > 0) {
            ElementMatcher.Junction<NamedElement> matcher = none();
            for (String name : mockJavaTime.value()) {
                matcher = matcher.or(nameStartsWith(name));
            }
            builder = builder
                    .type(matcher)
                    .transform((b, td, cl, module, domain) -> b
                            .visit(MemberSubstitution.relaxed()
                                    .method(is(currentTimeMillis))
                                    .replaceWith(newCurrentTimeMillis)
                                    .on(any()))
                            .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(td)));
        }

        return builder;
    }

    @Override
    public void onSpringContextReady(org.springframework.beans.factory.BeanFactory beanFactory) {
        if (!initialized) {
            return;
        }
        DateTimeService dateTimeService = beanFactory.getBean(DateTimeService.class);
        JavaTimeAdvice.setCallRealMethod(() -> !MockAnswer.isEnabled());
        JavaTimeAdvice.setCurrentTimeMillis(() -> dateTimeService.getDateTime().toInstant().toEpochMilli());
        JavaTimeAdvice.setGetNanoTimeAdjustment(o -> ChronoUnit.NANOS.between(Instant.ofEpochSecond(o),
                dateTimeService.getDateTime().toInstant()));
        JavaTimeAdvice.setGetDefaultRef(() -> TimeZone.getTimeZone(dateTimeService.getDateTime().getOffset()));
    }

    @Override
    public void cleanup() {
        if (initialized) {
            JavaTimeAdvice.setCallRealMethod(null);
            JavaTimeAdvice.setCurrentTimeMillis(null);
            JavaTimeAdvice.setGetNanoTimeAdjustment(null);
            JavaTimeAdvice.setGetDefaultRef(null);
            initialized = false;
        }
    }

    private static void initialize() throws Exception {
        realGetNanoTimeAdjustmentMethod = ByteBuddyUtils.makeAccessible(Class.forName("jdk.internal.misc.VM")
                .getDeclaredMethod("getNanoTimeAdjustment", long.class));
        JavaTimeAdvice.setRealGetNanoTimeAdjustment(SneakyFunction
                .sneaky(o -> (Long) realGetNanoTimeAdjustmentMethod.invoke(null, o)));

        realGetDefaultRefMethod = ByteBuddyUtils.makeAccessible(TimeZone.class.getDeclaredMethod("getDefaultRef"));
        JavaTimeAdvice.setRealGetDefaultRef(SneakySupplier.sneaky(() -> (TimeZone) realGetDefaultRefMethod.invoke(null)));
    }
}
