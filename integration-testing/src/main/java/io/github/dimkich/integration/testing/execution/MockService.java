package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import io.github.sugarcubes.cloner.Cloner;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.ScopedMock;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for creating and registering different Mockito-based mocks
 * (constructor, static and Spring bean mocks) used during integration tests.
 * <p>
 * Mocks configured via {@link JunitExtension} are created after Spring context
 * initialization and closed when the context is destroyed.
 */
@Service
@RequiredArgsConstructor
public class MockService {
    private final MockInvokeProperties properties;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private Cloner cloner;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private TestExecutor testExecutor;

    private final Set<String> mocks = new HashSet<>();
    private final List<ScopedMock> scopedMocks = new ArrayList<>();

    /**
     * Registers constructor and static mocks declared in {@link JunitExtension}.
     * <p>
     * For constructor mocks, the real object created by the original constructor
     * is stored inside {@link ConstructorMockAnswer} to delegate calls when needed.
     *
     * @throws ClassNotFoundException if a mocked class cannot be loaded
     */
    @PostConstruct
    public void postConstruct() throws ClassNotFoundException {
        MockAnswerBuilder builder = new MockAnswerBuilder();
        for (TestConstructorMock constructorMock : JunitExtension.getConstructorMocks()) {
            builder.from(constructorMock);
            MockedConstruction<?> mockedConstruction = Mockito.mockConstruction(builder.buildClass(),
                    Mockito.withSettings().defaultAnswer(new ConstructorMockAnswer(builder.buildAnswer())).stubOnly(),
                    (mock, context) -> {
                        Constructor<?> constructor = context.constructor();
                        ByteBuddyUtils.makeAccessible(constructor);
                        ConstructorMockAnswer ans = (ConstructorMockAnswer) MockUtil.getMockSettings(mock)
                                .getDefaultAnswer();
                        ans.getMockToObject().put(mock, constructor.newInstance(context.arguments().toArray()));
                    });
            scopedMocks.add(mockedConstruction);
        }
        for (TestStaticMock staticMock : JunitExtension.getStaticMocks()) {
            builder.from(staticMock);
            MockedStatic<?> mockedStatic = Mockito.mockStatic(builder.buildClass(), Mockito.withSettings()
                    .defaultAnswer(builder.buildAnswer()).stubOnly());
            scopedMocks.add(mockedStatic);
        }
    }

    /**
     * Creates a Mockito mock for a Spring bean according to {@link TestBeanMock}
     * configuration.
     *
     * @param beanMock definition of the bean mock to create
     * @param bean     original Spring bean instance that can be used as a spy target
     * @param beanName Spring bean name used as a unique mock identifier
     * @return created Mockito mock (possibly a spy of the original bean)
     */
    public Object createBeanMock(TestBeanMock beanMock, Object bean, String beanName) {
        MockAnswerBuilder builder = new MockAnswerBuilder();
        builder.from(beanMock, beanName);
        return Mockito.mock(bean.getClass(), Mockito.withSettings().defaultAnswer(builder.buildAnswer())
                .spiedInstance(bean).stubOnly());
    }

    /**
     * Closes all registered scoped mocks (constructor and static) to clean up
     * Mockito state when the Spring context is destroyed.
     */
    @PreDestroy
    public void preDestroy() {
        scopedMocks.forEach(ScopedMock::close);
    }

    /**
     * Helper class that builds {@link MockAnswer} instances and resolves
     * target classes and configuration from different mock annotations.
     */
    class MockAnswerBuilder {
        private String name;
        private Class<?> mockClass;
        private String mockClassName;
        private String[] methods;
        private boolean spy;
        private boolean cloneArgsAndResult;

        /**
         * Initializes builder fields from a {@link TestBeanMock} definition.
         *
         * @param mock     bean mock definition
         * @param beanName Spring bean name used as a mock name
         */
        public void from(TestBeanMock mock, String beanName) {
            name = beanName;
            mockClass = mock.mockClass();
            mockClassName = mock.mockClassName();
            methods = mock.methods();
            spy = mock.spy();
            cloneArgsAndResult = mock.cloneArgsAndResult();
        }

        /**
         * Initializes builder fields from a {@link TestConstructorMock} definition.
         *
         * @param mock constructor mock definition
         */
        public void from(TestConstructorMock mock) {
            name = mock.name();
            mockClass = mock.mockClass();
            mockClassName = mock.mockClassName();
            methods = mock.methods();
            spy = mock.spy();
            cloneArgsAndResult = mock.cloneArgsAndResult();
        }

        /**
         * Initializes builder fields from a {@link TestStaticMock} definition.
         *
         * @param mock static mock definition
         */
        public void from(TestStaticMock mock) {
            name = mock.name();
            mockClass = mock.mockClass();
            mockClassName = mock.mockClassName();
            methods = mock.methods();
            spy = mock.spy();
            cloneArgsAndResult = mock.cloneArgsAndResult();
        }

        /**
         * Resolves the class that should be mocked either from a direct reference
         * or from a fully qualified class name.
         *
         * @return class to be mocked
         * @throws ClassNotFoundException if the class name cannot be resolved
         */
        public Class<?> buildClass() throws ClassNotFoundException {
            mockClass = mockClass == Null.class ? Class.forName(mockClassName) : mockClass;
            return mockClass;
        }

        /**
         * Builds a {@link MockAnswer} and registers the mock name to prevent
         * duplicate registrations.
         *
         * @return configured {@link MockAnswer} instance
         * @throws IllegalArgumentException if a mock with the same name is already registered
         */
        public MockAnswer buildAnswer() {
            name = name.isEmpty() ? mockClass.getSimpleName() : name;
            if (mocks.contains(name)) {
                throw new IllegalArgumentException(String.format("Mock '%s' is already registered", name));
            }
            mocks.add(name);
            Set<String> methods = this.methods.length == 0 ? null : Set.of(this.methods);
            return new MockAnswer(name, methods, properties, testExecutor, cloner, spy, cloneArgsAndResult);
        }
    }
}
