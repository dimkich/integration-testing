package io.github.dimkich.integration.testing.execution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;
import org.assertj.core.util.introspection.IntrospectionError;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.BiPredicate;

/**
 * Describes a single stubbed call configuration.
 * <p>
 * The instance holds a method name, arguments and a sequence of {@link StubCallResult}
 * objects that should be returned or thrown on subsequent invocations. Arguments are
 * compared using AssertJ's recursive comparison, with the possibility to register
 * custom equality implementations for specific types.
 */
@Data
@Accessors(chain = true)
public class StubCall {
    private static final RecursiveComparisonDifferenceCalculator compCalculator =
            new RecursiveComparisonDifferenceCalculator();
    private static final RecursiveComparisonConfiguration compConfig = new RecursiveComparisonConfiguration();

    static {
        compConfig.registerEqualsForType((amr1, amr2) ->
                recursiveEquals(amr1.getReference(), amr2.getReference()), AtomicMarkableReference.class);
        compConfig.registerEqualsForType((a, b) -> a.compareTo(b) == 0, BigDecimal.class);
        compConfig.registerEqualsForType((a, b) -> a.toInstant().equals(b.toInstant()), ZonedDateTime.class);
        compConfig.registerEqualsForType(Temporal::equals, Temporal.class);
        compConfig.registerEqualsForType(StubCall::mapsEquals, Map.class);
    }

    /**
     * Stub name. Usually corresponds to a bean or component name.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    /**
     * Stubbed method name.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String method;
    /**
     * If {@code true}, argument comparison is skipped and only name/method are checked.
     */
    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean disabled;
    /**
     * Expected arguments for the invocation; {@code null} is allowed.
     */
    private List<Object> arg;
    /**
     * Sequence of results/exceptions that will be produced by this stub.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<StubCallResult> result;
    @JsonIgnore
    private int resultIndex = 0;
    /**
     * Whether this call was used during the current test run.
     * <p>
     * A call is marked as used when it matches a stub invocation or when it is recorded
     * after a real method invocation. Unused calls are removed from leaf tests when the
     * test is updated.
     */
    @JsonIgnore
    private boolean used;

    /**
     * Registers a custom equality predicate for the given type that will be used
     * by the recursive comparison when comparing arguments.
     *
     * @param type   class for which the predicate should be applied
     * @param equals bi-predicate that returns {@code true} when values are equal
     */
    @SuppressWarnings("unchecked")
    public static void addEqualsForType(Class<?> type, BiPredicate<?, ?> equals) {
        compConfig.registerEqualsForType((BiPredicate<Object, Object>) equals, (Class<Object>) type);
    }

    /**
     * Appends a normal return value to the list of results.
     *
     * @param result value that should be returned on invocation
     */
    public void addResult(Object result) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(new StubCallResult(result));
    }

    /**
     * Appends an exception to the list of results.
     *
     * @param e exception that should be thrown on invocation
     */
    public void addException(Throwable e) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(new StubCallResult(e));
    }

    /**
     * Resets the internal result index so that subsequent invocations start
     * from the first configured {@link StubCallResult}.
     */
    @JsonIgnore
    public void reset() {
        resultIndex = 0;
    }

    /**
     * Returns the current configured result value for this invocation and
     * advances the internal index. If no results are configured, {@code null}
     * is returned.
     *
     * @return current result value or {@code null} if none
     */
    @JsonIgnore
    public Object getCurrentResult() {
        if (this.result == null) {
            return null;
        }
        StubCallResult r = result.get(resultIndex % result.size());
        resultIndex++;
        return r.getReturn1();
    }

    /**
     * If the current {@link StubCallResult} contains an exception, advances
     * the internal index and throws that exception.
     *
     * @throws Throwable when the current result is configured with an exception
     */
    @JsonIgnore
    public void tryThrowException() throws Throwable {
        if (this.result == null) {
            return;
        }
        StubCallResult r = result.get(resultIndex % result.size());
        if (r.getThrow1() != null) {
            resultIndex++;
            throw r.getThrow1();
        }
    }

    /**
     * Checks whether this stub configuration matches the supplied name, method and arguments.
     * <ul>
     *     <li>Names and methods are compared using {@link Objects#equals(Object, Object)}.</li>
     *     <li>If {@link #disabled} is {@code true}, arguments are ignored.</li>
     *     <li>If enabled, arguments are compared pairwise using recursive comparison
     *     (with registered custom equals predicates) or direct {@code equals} where applicable.</li>
     * </ul>
     *
     * @param name   stub name
     * @param method method name
     * @param arg    actual arguments of the invocation
     * @return {@code true} if this configuration matches the supplied data
     */
    @SneakyThrows
    public boolean equalsTo(String name, String method, List<Object> arg) {
        if (!Objects.equals(this.name, name) || !Objects.equals(this.method, method)) {
            return false;
        }
        if (disabled) {
            return true;
        }
        if (this.arg == null || arg == null) {
            return this.arg == arg;
        }
        if (this.arg.size() != arg.size()) {
            return false;
        }
        for (int i = 0; i < this.arg.size(); i++) {
            if (!isEquals(this.arg.get(i), arg.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEquals(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        }
        return recursiveEquals(o1, o2);
    }

    /**
     * Compares two non-null values with AssertJ's recursive comparison.
     * <p>
     * Some JDK value types (for example {@code java.time} classes) cannot be introspected when
     * the {@code java.base} module is not opened for deep reflection. In that case AssertJ fails
     * with {@link IntrospectionError} before it checks the comparators registered in
     * {@link #compConfig}, so the values are compared with {@link Object#equals(Object)} instead.
     *
     * @param actual   actual value
     * @param expected expected value
     * @return {@code true} if the values are equal
     */
    private static boolean recursiveEquals(Object actual, Object expected) {
        try {
            return compCalculator.determineDifferences(actual, expected, compConfig).isEmpty();
        } catch (IntrospectionError e) {
            return Objects.equals(actual, expected);
        }
    }

    /**
     * Compares two maps like AssertJ's recursive comparison does, except that keys are matched
     * with the registered equality predicates instead of {@link Object#equals(Object)}.
     * <p>
     * AssertJ always matches map keys by {@code equals}, even when a custom comparator is registered
     * for the key type, so maps with {@link BigDecimal} keys {@code 0.1} and {@code 0.10}
     * would be reported as different.
     * <p>
     * This is a known AssertJ limitation, see
     * <a href="https://github.com/assertj/assertj/issues/3835">assertj#3835</a>
     * ("RecursiveComparisonConfiguration is not taken into account for map keys"). It is fixed
     * in AssertJ 4.0.0-M2 and later, where map keys are compared with the configured
     * recursive comparison; after upgrading to that version this method can be removed.
     *
     * @param actual   actual map
     * @param expected expected map
     * @return {@code true} if the maps have the same size and equal entries
     */
    private static boolean mapsEquals(Map<?, ?> actual, Map<?, ?> expected) {
        if (actual.size() != expected.size()) {
            return false;
        }
        if (hasSameKeys(actual, expected)) {
            for (Map.Entry<?, ?> actualEntry : actual.entrySet()) {
                if (!isEquals(actualEntry.getValue(), expected.get(actualEntry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        List<Map.Entry<?, ?>> unmatched = new ArrayList<>(expected.entrySet());
        for (Map.Entry<?, ?> actualEntry : actual.entrySet()) {
            Map.Entry<?, ?> expectedEntry = removeEntryWithEqualKey(unmatched, actualEntry.getKey());
            if (expectedEntry == null || !isEquals(actualEntry.getValue(), expectedEntry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether both maps contain the same keys according to their own lookup rules,
     * so that values can be compared by {@link Map#get(Object)} without scanning the entries.
     */
    private static boolean hasSameKeys(Map<?, ?> actual, Map<?, ?> expected) {
        if (!actual.keySet().equals(expected.keySet())) {
            return false;
        }
        for (Object key : actual.keySet()) {
            if (!expected.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    private static Map.Entry<?, ?> removeEntryWithEqualKey(List<Map.Entry<?, ?>> entries, Object key) {
        for (Iterator<Map.Entry<?, ?>> iterator = entries.iterator(); iterator.hasNext(); ) {
            Map.Entry<?, ?> entry = iterator.next();
            if (isEquals(entry.getKey(), key)) {
                iterator.remove();
                return entry;
            }
        }
        return null;
    }
}
