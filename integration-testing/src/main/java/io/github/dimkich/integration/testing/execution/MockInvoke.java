package io.github.dimkich.integration.testing.execution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.BiPredicate;

/**
 * Describes a single mock invocation configuration.
 * <p>
 * The instance holds a method name, arguments and a sequence of {@link MockInvokeResult}
 * objects that should be returned or thrown on subsequent invocations. Arguments are
 * compared using AssertJ's recursive comparison, with the possibility to register
 * custom equality implementations for specific types.
 */
@Data
@Accessors(chain = true)
public class MockInvoke {
    private static final RecursiveComparisonDifferenceCalculator compCalculator =
            new RecursiveComparisonDifferenceCalculator();
    private static final RecursiveComparisonConfiguration compConfig = new RecursiveComparisonConfiguration();

    static {
        compConfig.registerEqualsForType((amr1, amr2) -> compCalculator.determineDifferences(amr1.getReference(),
                amr2.getReference(), compConfig).isEmpty(), AtomicMarkableReference.class);
    }

    /**
     * Mock name. Usually corresponds to a bean or component name.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    /**
     * Mocked method name.
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
     * Expected arguments for the invocation (may be {@code null}).
     */
    private List<Object> arg;
    /**
     * Sequence of results/exceptions that will be produced by this mock.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<MockInvokeResult> result;
    @JsonIgnore
    private int resultIndex = 0;

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
        this.result.add(new MockInvokeResult(result));
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
        this.result.add(new MockInvokeResult(e));
    }

    @JsonIgnore
    /**
     * Resets the internal result index so that subsequent invocations start
     * from the first configured {@link MockInvokeResult}.
     */
    public void reset() {
        resultIndex = 0;
    }

    @JsonIgnore
    /**
     * Returns the current configured result value for this invocation and
     * advances the internal index. If no results are configured, {@code null}
     * is returned.
     *
     * @return current result value or {@code null} if none
     */
    public Object getCurrentResult() {
        if (this.result == null) {
            return null;
        }
        MockInvokeResult r = result.get(resultIndex % result.size());
        resultIndex++;
        return r.getReturn1();
    }

    @JsonIgnore
    /**
     * If the current {@link MockInvokeResult} contains an exception, advances
     * the internal index and throws that exception.
     *
     * @throws Throwable when the current result is configured with an exception
     */
    public void tryThrowException() throws Throwable {
        if (this.result == null) {
            return;
        }
        MockInvokeResult r = result.get(resultIndex % result.size());
        if (r.getThrow1() != null) {
            resultIndex++;
            throw r.getThrow1();
        }
    }

    @SneakyThrows
    /**
     * Checks whether this mock configuration matches the supplied name, method and arguments.
     * <ul>
     *     <li>Names and methods are compared using {@link Objects#equals(Object, Object)}.</li>
     *     <li>If {@link #disabled} is {@code true}, arguments are ignored.</li>
     *     <li>If enabled, arguments are compared pairwise using recursive comparison
     *     (with registered custom equals predicates) or direct {@code equals} where applicable.</li>
     * </ul>
     *
     * @param name   mock name
     * @param method method name
     * @param arg    actual arguments of the invocation
     * @return {@code true} if this configuration matches the supplied data
     */
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

    private boolean isEquals(Object o1, Object o2) throws NoSuchMethodException {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        }
        if (o1.getClass() == o1.getClass().getMethod("equals", Object.class).getDeclaringClass()) {
            return Objects.equals(o1, o2);
        }
        return compCalculator.determineDifferences(o1, o2, compConfig).isEmpty();
    }
}
