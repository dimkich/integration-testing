package io.github.dimkich.integration.testing.comparison;

import io.github.dimkich.integration.testing.TestSetupModule;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;
import org.assertj.core.util.introspection.IntrospectionError;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Compares object graphs using AssertJ's recursive comparison with equality predicates
 * declared in {@link TestSetupModule}s.
 * <p>
 * Built-in presets are provided by {@link ComparisonConfig} through the default
 * {@code comparisonTestSetupModule}. The service itself only collects predicates from
 * all available modules and delegates the actual comparison to AssertJ.
 */
public class ObjectComparisonService {
    private final RecursiveComparisonConfiguration configuration = new RecursiveComparisonConfiguration();
    private final RecursiveComparisonDifferenceCalculator calculator = new RecursiveComparisonDifferenceCalculator();

    /**
     * Creates a comparison service configured with the equality predicates from the given modules.
     *
     * @param modules modules whose {@code equalsMap} entries should be registered
     */
    public ObjectComparisonService(List<TestSetupModule> modules) {
        for (TestSetupModule module : modules) {
            module.getEqualsMap().forEach(this::registerEquals);
        }
    }

    /**
     * Recursively checks whether two objects are equal according to the configured rules.
     *
     * @param actual   actual value
     * @param expected expected value
     * @return {@code true} if the values are equal
     */
    public boolean isEquals(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return actual == expected;
        }
        try {
            return calculator.determineDifferences(actual, expected, configuration).isEmpty();
        } catch (IntrospectionError e) {
            return Objects.equals(actual, expected);
        }
    }

    /**
     * Registers a predicate with centralized null handling.
     * The predicate is only invoked when both values are non-null.
     */
    @SuppressWarnings("unchecked")
    private void registerEquals(Class<?> type, BiPredicate<?, ?> equals) {
        BiPredicate<Object, Object> predicate = (BiPredicate<Object, Object>) equals;
        configuration.registerEqualsForType((actual, expected) -> {
            if (actual == null || expected == null) {
                return actual == expected;
            }
            return predicate.test(actual, expected);
        }, (Class<Object>) type);
    }
}
