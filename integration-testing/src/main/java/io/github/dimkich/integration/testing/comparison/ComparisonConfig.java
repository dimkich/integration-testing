package io.github.dimkich.integration.testing.comparison;

import io.github.dimkich.integration.testing.TestSetupModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

/**
 * Configuration of the object comparison subsystem.
 * <p>
 * Registers the {@link ObjectComparisonService} and a default {@link TestSetupModule}
 * with built-in equality rules.
 */
@Configuration
@Import(ObjectComparisonService.class)
public class ComparisonConfig {
    /**
     * Creates the default comparison module with built-in equality presets.
     * <p>
     * Recursive presets receive a lazy reference to the comparison service to break the
     * dependency cycle between the service and the default module.
     *
     * @param comparisonService lazily resolved comparison service
     * @return module with built-in equality rules
     */
    @Bean
    TestSetupModule comparisonTestSetupModule(ObjectProvider<ObjectComparisonService> comparisonService) {
        BiPredicate<Object, Object> recursiveEquals = (actual, expected) ->
                comparisonService.getObject().isEquals(actual, expected);
        return new TestSetupModule()
                .addEqualsForType(AtomicMarkableReference.class,
                        castPredicate(new AtomicMarkableReferenceEquals(recursiveEquals)))
                .addEqualsForType(AtomicReference.class,
                        castPredicate(new AtomicReferenceEquals(recursiveEquals)))
                .addEqualsForType(BigDecimal.class, new BigDecimalEquals())
                .addEqualsForType(ZonedDateTime.class, new ZonedDateTimeEquals())
                .addEqualsForType(Temporal.class, new TemporalEquals())
                .addEqualsForType(Map.class, castPredicate(new MapEquals(recursiveEquals)));
    }

    @SuppressWarnings("unchecked")
    private static <T> BiPredicate<? super T, ? super T> castPredicate(BiPredicate<?, ?> predicate) {
        return (BiPredicate<? super T, ? super T>) predicate;
    }
}
