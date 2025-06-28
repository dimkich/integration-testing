package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.message.MessageDto;
import lombok.Getter;
import lombok.Setter;
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

@Getter
@Setter
@JsonRootName(value = "testCase")
@JsonPropertyOrder({"name", "init", "bean", "method", "request", "inboundMessage", "mockInvoke", "response",
        "dataStorageDiff", "outboundMessages", "testCase"})
public class TestCase {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JsonProperty("init")
    @JsonManagedReference
    private List<TestCaseInit> inits = new ArrayList<>();
    private String bean;
    private String method;
    private List<Object> request;
    private MessageDto<?> inboundMessage;
    private List<MockInvoke> mockInvoke = new ArrayList<>();
    private Object response;
    private Object dataStorageDiff;
    @JsonProperty("outboundMessage")
    private List<MessageDto<?>> outboundMessages;
    @JsonBackReference
    private TestCase parentTestCase;
    @JsonManagedReference
    @JsonProperty("testCase")
    private List<TestCase> subTestCases = new ArrayList<>();

    @JsonIgnore
    public boolean isContainer() {
        return !subTestCases.isEmpty();
    }

    @JsonIgnore
    public boolean isLastLeaf() {
        if (parentTestCase == null) {
            return true;
        }
        return parentTestCase.getSubTestCases().get(parentTestCase.getSubTestCases().size() - 1) == this;
    }

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

    public MockInvoke search(String mockName, String method, List<Object> args) {
        return mockInvoke.stream()
                .filter(mi -> mi.equalsTo(mockName, method, args))
                .findFirst()
                .orElse(null);
    }

    @JsonIgnore
    public Stream<TestCase> getParentsAndItselfAsc() {
        if (parentTestCase == null) {
            return Stream.of(this);
        }
        return Stream.concat(parentTestCase.getParentsAndItselfAsc(), Stream.of(this));
    }

    @JsonIgnore
    public Stream<TestCase> getParentsAndItselfDesc() {
        if (parentTestCase == null) {
            return Stream.of(this);
        }
        return Stream.concat(Stream.of(this), parentTestCase.getParentsAndItselfDesc());
    }

    @JsonIgnore
    public String getFullName() {
        return getParentsAndItselfAsc()
                .map(TestCase::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" - "));
    }

    @JsonIgnore
    public int getLevel() {
        if (parentTestCase == null) {
            return 0;
        }
        return parentTestCase.getLevel() + 1;
    }

    public Optional<TestCase> filter(Predicate<TestCase> predicate) throws Exception {
        Objects.requireNonNull(predicate);
        return predicate.test(this) ? Optional.of(this) : Optional.empty();
    }

    public Optional<TestCase> filterLevel(int level) {
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
