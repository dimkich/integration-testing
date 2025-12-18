package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.initialization.TestInit;
import io.github.dimkich.integration.testing.message.MessageDto;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.test.util.AopTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract base class representing a test in the integration testing framework.
 * <p>
 * This class supports a hierarchical test structure with three types:
 * <ul>
 *   <li><b>{@link TestContainer}</b> - A container that groups test cases or other containers. Can be the root or a child of another TestContainer</li>
 *   <li><b>{@link TestCase}</b> - A test case that must be a child of a TestContainer</li>
 *   <li><b>{@link TestPart}</b> - A test part that must be a child of a TestCase</li>
 * </ul>
 * Each test can:
 * <ul>
 *   <li>Execute methods on Spring beans using reflection</li>
 *   <li>Handle inbound and outbound messages</li>
 *   <li>Define mock invocations for verification</li>
 *   <li>Store custom data for test-specific needs</li>
 *   <li>Track data storage differences</li>
 *   <li>Be disabled individually or inherit disabled state from parent</li>
 * </ul>
 * <p>
 * Tests support initialization through {@link TestInit} configurations and maintain
 * parent-child relationships for proper lifecycle management (before/after hooks).
 * <p>
 * This class is designed to be serialized/deserialized from XML/JSON test configurations
 * using Jackson annotations.
 *
 * @author dimkich
 * @see TestInit
 * @see MockInvoke
 * @see MessageDto
 * @see TestContainer
 * @see TestCase
 * @see TestPart
 */
@Getter
@Setter
@FieldNameConstants
@JsonRootName(value = "test")
@JsonPropertyOrder({"name", "init", "bean", "method", "request", "inboundMessage", "mockInvoke", "response",
        "custom", "dataStorageDiff", "outboundMessages", "test"})
public abstract class Test {
    /**
     * Enumeration of test types in the hierarchical test structure.
     * <p>
     * The hierarchy must follow: {@link TestContainer} → ({@link TestContainer} | {@link TestCase}) → {@link TestCase} → {@link TestPart}
     */
    public enum Type {
        /**
         * Container that groups test cases or other containers. Can be the root or a child of another {@link TestContainer}
         */
        TestContainer,
        /**
         * Test case that must be a child of a {@link TestContainer}
         */
        TestCase,
        /**
         * Test part that must be a child of a {@link TestCase}
         */
        TestPart
    }

    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean disabled;
    @JsonProperty("init")
    @JsonManagedReference
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<TestInit> inits = new ArrayList<>();
    private String bean;
    private String method;
    private List<Object> request;
    private MessageDto<?> inboundMessage;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<MockInvoke> mockInvoke = new ArrayList<>();
    private Object response;
    @Getter(value = AccessLevel.PACKAGE, onMethod_ = @JsonProperty)
    @Setter(value = AccessLevel.PACKAGE, onMethod_ = @JsonProperty)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> custom;
    private Object dataStorageDiff;
    @JsonProperty("outboundMessage")
    private List<MessageDto<?>> outboundMessages;
    @JsonBackReference
    private Test parentTest;
    @JsonManagedReference
    @JsonProperty("test")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Test> subTests = new ArrayList<>();
    @JsonIgnore
    private boolean initialized;
    @JsonIgnore
    private Boolean calculatedDisabled;
    @JsonIgnore
    private int lineNumber;
    @JsonIgnore
    private int columnNumber;

    /**
     * Returns the type of this test.
     *
     * @return the test type ({@link TestContainer}, {@link TestCase}, or {@link TestPart})
     */
    @JsonIgnore
    public abstract Type getType();

    /**
     * Checks if this test is a container (has sub-tests).
     *
     * @return true if this test has sub-tests, false otherwise
     */
    @JsonIgnore
    public boolean isContainer() {
        return !subTests.isEmpty();
    }

    /**
     * Validates the test hierarchy structure.
     * <p>
     * Ensures that:
     * <ul>
     *   <li>The root test is of type {@link TestContainer}</li>
     *   <li>{@link TestContainer} can be a child of {@link TestContainer} (nested containers)</li>
     *   <li>{@link TestCase} can only be a child of {@link TestContainer}</li>
     *   <li>{@link TestPart} can only be a child of {@link TestCase}</li>
     * </ul>
     *
     * @throws RuntimeException if the hierarchy structure is invalid
     */
    @JsonIgnore
    public void check() {
        if (parentTest == null && getType() != Type.TestContainer) {
            throw new RuntimeException("Root <test> must be of type=\"container\"");
        }
        if (getType() == Type.TestCase && parentTest.getType() != Type.TestContainer) {
            throw new RuntimeException("<test type=\"case\"> can only be a child of <test type=\"container\">");
        }
        if (getType() == Type.TestPart && parentTest.getType() != Type.TestCase) {
            throw new RuntimeException("<test type=\"part\"> can only be a child of <test type=\"case\">");
        }
    }

    /**
     * Executes the before hook for this test and its parent hierarchy.
     * <p>
     * If the test is already initialized, it will fix the after hooks for sub-tests.
     * Otherwise, it recursively calls before on the parent test, validates the test structure,
     * executes the before consumer, and marks the test as initialized.
     *
     * @param before the consumer to execute before the test runs
     * @param after the consumer to execute after the test runs (used for fixing sub-tests)
     * @throws Exception if an error occurs during execution
     */
    public void before(SneakyConsumer<Test, Exception> before, SneakyConsumer<Test, Exception> after) throws Exception {
        if (initialized) {
            fixAfter(after);
        } else {
            if (parentTest != null) {
                parentTest.before(before, after);
            }
            check();
            before.accept(this);
            initialized = true;
        }
    }

    private void fixAfter(SneakyConsumer<Test, Exception> after) throws Exception {
        for (Test test : subTests) {
            if (test.initialized) {
                test.fixAfter(after);
                if (test.initialized) {
                    test.after(after, null);
                }
            }
        }
    }

    /**
     * Executes the after hook for this test and propagates to parent if needed.
     * <p>
     * If the test is initialized, executes the after consumer and marks it as uninitialized.
     * If this is the last leaf test or matches the lastTest parameter, propagates the after
     * hook to the parent test.
     *
     * @param after the consumer to execute after the test runs
     * @param lastTest the last test that was executed, used to determine if parent should be called
     * @throws Exception if an error occurs during execution
     */
    public void after(SneakyConsumer<Test, Exception> after, Test lastTest) throws Exception {
        if (initialized) {
            after.accept(this);
            initialized = false;
        }
        if ((lastTest == this || isLastLeaf()) && parentTest != null) {
            parentTest.after(after, lastTest == this ? parentTest : lastTest);
        }
    }

    /**
     * Gets the calculated disabled state of this test.
     * <p>
     * A test is disabled if it is explicitly disabled or if any of its parent tests are disabled.
     * The result is cached after the first calculation.
     *
     * @return true if this test or any parent is disabled, false otherwise
     */
    public Boolean getCalculatedDisabled() {
        if (calculatedDisabled == null) {
            calculatedDisabled = disabled ? disabled : parentTest != null && parentTest.getCalculatedDisabled();
        }
        return calculatedDisabled;
    }

    @JsonIgnore
    public boolean isFirstLeaf() {
        if (parentTest == null) {
            return true;
        }
        return parentTest.getSubTests().get(0) == this;
    }

    /**
     * Checks if this test is the last leaf in its parent's sub-tests list.
     * <p>
     * A root test (no parent) is considered the last leaf.
     *
     * @return true if this is the last child of its parent, or if it has no parent
     */
    @JsonIgnore
    public boolean isLastLeaf() {
        if (parentTest == null) {
            return true;
        }
        return parentTest.getSubTests().get(parentTest.getSubTests().size() - 1) == this;
    }

    /**
     * Adds a custom key-value pair to this test's custom data map.
     * <p>
     * The custom map is initialized as a TreeMap if it doesn't exist.
     *
     * @param key the key to store the value under
     * @param value the value to store
     */
    public void addCustom(String key, Object value) {
        if (custom == null) {
            custom = new TreeMap<>();
        }
        custom.put(key, value);
    }

    /**
     * Retrieves a custom value by key from this test's custom data map.
     *
     * @param key the key to look up
     * @return the value associated with the key, or null if the key doesn't exist or custom map is null
     */
    public Object getCustom(String key) {
        return custom == null ? null : custom.get(key);
    }

    /**
     * Clears all custom data from this test's custom data map.
     * <p>
     * Does nothing if the custom map is null.
     */
    public void clearCustom() {
        if (custom != null) {
            custom.clear();
        }
    }

    /**
     * Executes the method specified by {@code bean} and {@code method} using reflection.
     * <p>
     * The method is found by matching the method name and parameter types with the provided
     * request arguments. The method is invoked on the bean retrieved from the BeanFactory.
     * <p>
     * If the method throws an exception, the exception is caught and converted using the
     * responseConverter. Otherwise, the return value is converted.
     *
     * @param beanFactory the Spring BeanFactory to retrieve the target bean from
     * @param responseConverter a function to convert the method result or exception to the response format
     * @throws IllegalAccessException if the method cannot be accessed
     * @throws RuntimeException if the method is not found in the bean
     */
    public void executeMethod(BeanFactory beanFactory, BiFunction<String, Object, Object> responseConverter) throws IllegalAccessException {
        Object target = beanFactory.getBean(bean);
        Method reflectionMethod = findReflectionMethodByArgs(target, method, request);
        if (reflectionMethod == null) {
            throw new RuntimeException(String.format("Method %s not found in bean %s", method, bean));
        }
        Object[] args = request == null ? new Object[]{} : request.toArray();
        try {
            response = responseConverter.apply(method, reflectionMethod.invoke(target, args));
        } catch (InvocationTargetException e) {
            response = responseConverter.apply(method, e.getTargetException());
        }
    }

    /**
     * Searches for a mock invocation matching the given criteria.
     *
     * @param mockName the name of the mock
     * @param method the method name that was invoked
     * @param args the arguments that were passed to the method
     * @return the matching MockInvoke, or null if no match is found
     */
    public MockInvoke search(String mockName, String method, List<Object> args) {
        return mockInvoke.stream()
                .filter(mi -> mi.equalsTo(mockName, method, args))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns a stream of this test and all its parents in ascending order (root to this).
     * <p>
     * The stream starts with the root test and ends with this test.
     *
     * @return a stream containing this test and all its parents, from root to this
     */
    @JsonIgnore
    public Stream<Test> getParentsAndItselfAsc() {
        if (parentTest == null) {
            return Stream.of(this);
        }
        return Stream.concat(parentTest.getParentsAndItselfAsc(), Stream.of(this));
    }

    /**
     * Returns a stream of this test and all its parents in descending order (this to root).
     * <p>
     * The stream starts with this test and ends with the root test.
     *
     * @return a stream containing this test and all its parents, from this to root
     */
    @JsonIgnore
    public Stream<Test> getParentsAndItselfDesc() {
        if (parentTest == null) {
            return Stream.of(this);
        }
        return Stream.concat(Stream.of(this), parentTest.getParentsAndItselfDesc());
    }

    /**
     * Returns the full name of this test by concatenating all parent names and this test's name.
     * <p>
     * Names are quoted and joined with commas. Null names are filtered out.
     * Example: "Container", "Case", "Part"
     *
     * @return a comma-separated string of all test names in the hierarchy from root to this
     */
    @JsonIgnore
    public String getFullName() {
        return getParentsAndItselfAsc()
                .map(Test::getName)
                .filter(Objects::nonNull)
                .map(n -> '"' + n + '"')
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns the depth level of this test in the hierarchy.
     * <p>
     * Root tests (no parent) are at level 0. Each level down increases the level by 1.
     *
     * @return the depth level (0 for root, 1 for first level children, etc.)
     */
    @JsonIgnore
    public int getLevel() {
        if (parentTest == null) {
            return 0;
        }
        return parentTest.getLevel() + 1;
    }

    /**
     * Filters this test based on the given predicate.
     * <p>
     * Returns an Optional containing this test if the predicate matches, empty otherwise.
     *
     * @param predicate the predicate to test this test against
     * @return Optional.of(this) if predicate matches, Optional.empty() otherwise
     * @throws NullPointerException if predicate is null
     */
    public Optional<Test> filter(Predicate<Test> predicate) throws Exception {
        Objects.requireNonNull(predicate);
        return predicate.test(this) ? Optional.of(this) : Optional.empty();
    }

    /**
     * Filters this test based on its level in the hierarchy.
     *
     * @param level the level to match against
     * @return Optional.of(this) if this test's level matches, Optional.empty() otherwise
     */
    public Optional<Test> filterLevel(int level) {
        return getLevel() == level ? Optional.of(this) : Optional.empty();
    }

    private Method findReflectionMethodByArgs(Object target, String method, List<Object> args) {
        args = args == null ? List.of() : args;
        List<Class<?>> types = args.stream()
                .map(a -> a == null ? null : a.getClass())
                .collect(Collectors.toList());
        return findReflectionMethodByTypes(target, method, types);
    }

    private Method findReflectionMethodByTypes(Object target, String method, List<Class<?>> types) {
        Class<?>[] argArray = types.toArray(Class[]::new);
        Class<?> targetClass = AopTestUtils.getUltimateTargetObject(target).getClass();
        return Arrays.stream(targetClass.getMethods())
                .filter(m -> m.getName().equals(method))
                .filter(m -> m.getParameterCount() == argArray.length)
                .filter(m -> compareClasses(m.getParameterTypes(), argArray))
                .findFirst()
                .orElse(null);
    }

    private boolean compareClasses(Class<?>[] classes1, Class<?>[] classes2) {
        classes1 = ClassUtils.primitivesToWrappers(classes1);
        for (int i = 0; i < classes1.length; i++) {
            if (classes2[i] != null && !classes1[i].isAssignableFrom(classes2[i])) {
                return false;
            }
        }
        return true;
    }
}
