package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.message.MessageDto;
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

@Getter
@Setter
@FieldNameConstants
@JsonRootName(value = "test")
@JsonPropertyOrder({"name", "init", "bean", "method", "request", "inboundMessage", "mockInvoke", "response",
        "dataStorageDiff", "outboundMessages", "test"})
public abstract class Test {
    public enum Type {TestContainer, TestCase, TestPart}

    @JacksonXmlProperty(isAttribute = true)
    private String name;
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
    public abstract Type getType();

    @JsonIgnore
    public boolean isContainer() {
        return !subTests.isEmpty();
    }

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

    @JsonIgnore
    public boolean isLastLeaf() {
        if (parentTest == null) {
            return true;
        }
        return parentTest.getSubTests().get(parentTest.getSubTests().size() - 1) == this;
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
    public Stream<Test> getParentsAndItselfAsc() {
        if (parentTest == null) {
            return Stream.of(this);
        }
        return Stream.concat(parentTest.getParentsAndItselfAsc(), Stream.of(this));
    }

    @JsonIgnore
    public Stream<Test> getParentsAndItselfDesc() {
        if (parentTest == null) {
            return Stream.of(this);
        }
        return Stream.concat(Stream.of(this), parentTest.getParentsAndItselfDesc());
    }

    @JsonIgnore
    public String getFullName() {
        return getParentsAndItselfAsc()
                .map(Test::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" - "));
    }

    @JsonIgnore
    public int getLevel() {
        if (parentTest == null) {
            return 0;
        }
        return parentTest.getLevel() + 1;
    }

    public Optional<Test> filter(Predicate<Test> predicate) throws Exception {
        Objects.requireNonNull(predicate);
        return predicate.test(this) ? Optional.of(this) : Optional.empty();
    }

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
