package io.github.dimkich.integration.testing.comparison;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.util.FastPath;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;
import org.assertj.core.util.introspection.ClassUtils;
import org.assertj.core.util.introspection.IntrospectionError;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Compares object graphs using AssertJ's recursive comparison with equality predicates
 * declared in {@link TestSetupModule}s.
 * <p>
 * Built-in presets are provided by {@link ComparisonConfig} through the default
 * {@code comparisonTestSetupModule}. The service itself only collects predicates from
 * all available modules and delegates the actual comparison to AssertJ. A user predicate is also
 * reported by {@link #hasCustomEquals(Class)}; a framework preset is only an equality rule and
 * never suppresses a difference on its own.
 * <p>
 * Scalars of one type are compared by {@code equals} before the recursive comparison runs: the
 * difference engine calls this method for every key of every map, and a recursive walk of a string
 * or a number has no advantage over equality. JDK value types and {@code byte[]}/{@code char[]}
 * take the same shortcut. A registered predicate is invoked directly: AssertJ applies it through
 * the recursive comparison machinery, which costs far more than the predicate itself when the
 * predicate covers the whole value, for example the built-in map predicate.
 */
public class ObjectComparisonService {
    private final RecursiveComparisonConfiguration configuration = new RecursiveComparisonConfiguration();
    private final RecursiveComparisonDifferenceCalculator calculator = new RecursiveComparisonDifferenceCalculator();
    private final Map<Class<?>, EqualsRule> rules = new LinkedHashMap<>();

    /**
     * Finds the rule of a type the same way AssertJ does: an exact registration, then a
     * superclass, then an interface. Resolved once per class, because the lookup runs for every
     * compared value.
     */
    private final ClassValue<ResolvedEquals> resolvedEquals = new ClassValue<>() {
        @Override
        protected ResolvedEquals computeValue(@NonNull Class<?> type) {
            EqualsRule exact = rules.get(type);
            if (exact != null) {
                return new ResolvedEquals(exact, false);
            }
            for (Class<?> superClass : ClassUtils.getAllSuperclasses(type)) {
                EqualsRule rule = rules.get(superClass);
                if (rule != null) {
                    return new ResolvedEquals(rule, false);
                }
            }
            for (Class<?> interfaceType : ClassUtils.getAllInterfaces(type)) {
                EqualsRule rule = rules.get(interfaceType);
                if (rule != null) {
                    return new ResolvedEquals(rule, false);
                }
            }
            return new ResolvedEquals(null, FastPath.isComparableByEquals(type));
        }
    };

    /**
     * Creates a comparison service configured with the equality predicates from the given modules.
     * <p>
     * Presets are registered first and user predicates second, so a user predicate of the same
     * type replaces the preset. Both are passed to AssertJ for recursive comparison.
     *
     * @param modules modules whose equality entries should be registered
     */
    public ObjectComparisonService(List<TestSetupModule> modules) {
        for (TestSetupModule module : modules) {
            module.getPresetEqualsMap().forEach((type, predicate) ->
                    register(type, predicate, EqualsKind.PRESET));
        }
        for (TestSetupModule module : modules) {
            module.getEqualsMap().forEach((type, predicate) ->
                    register(type, predicate, EqualsKind.USER));
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
        if (actual == expected) {
            return true;
        }
        Class<?> type = actual.getClass();
        if (type == expected.getClass()) {
            ResolvedEquals resolved = resolvedEquals.get(type);
            EqualsRule rule = resolved.rule();
            if (rule != null) {
                return rule.predicate().test(actual, expected);
            }
            if (type == byte[].class) {
                return Arrays.equals((byte[]) actual, (byte[]) expected);
            }
            if (type == char[].class) {
                return Arrays.equals((char[]) actual, (char[]) expected);
            }
            if (resolved.comparableByEquals()) {
                return actual.equals(expected);
            }
        }
        try {
            return calculator.determineDifferences(actual, expected, configuration).isEmpty();
        } catch (IntrospectionError e) {
            return Objects.equals(actual, expected);
        }
    }

    /**
     * Reports whether a user-registered equality predicate applies to a type. A framework preset
     * is not a custom predicate and is not reported: a preset never suppresses a difference on
     * its own, so the diff renderer still has to run. The check is cached per class, because it
     * runs for every compared value.
     *
     * @param type value type
     * @return {@code true} when a user predicate is selected for the type
     */
    public boolean hasCustomEquals(Class<?> type) {
        EqualsRule rule = resolvedEquals.get(type).rule();
        return rule != null && rule.kind() == EqualsKind.USER;
    }

    /**
     * Registers a predicate with centralized null handling.
     * The predicate is only invoked when both values are non-null.
     */
    @SuppressWarnings("unchecked")
    private void register(Class<?> type, BiPredicate<?, ?> equals, EqualsKind kind) {
        BiPredicate<Object, Object> predicate = (BiPredicate<Object, Object>) equals;
        BiPredicate<Object, Object> nullablePredicate = (actual, expected) -> {
            if (actual == null || expected == null) {
                return actual == expected;
            }
            return predicate.test(actual, expected);
        };
        rules.put(type, new EqualsRule(nullablePredicate, kind));
        configuration.registerEqualsForType(nullablePredicate, (Class<Object>) type);
    }

    /**
     * Kind of an equality rule: a user predicate can suppress a difference, a preset is only an
     * equality rule for {@link #isEquals}.
     */
    private enum EqualsKind {
        USER,
        PRESET
    }

    private record EqualsRule(BiPredicate<Object, Object> predicate, EqualsKind kind) {
    }

    private record ResolvedEquals(EqualsRule rule, boolean comparableByEquals) {
    }
}
