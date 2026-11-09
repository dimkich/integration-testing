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
 * Service responsible for creating and registering Mockito-based stubs
 * (construction, static method and Spring bean stubs) used during integration tests.
 * <p>
 * Stubs configured via {@link JunitExtension} are created after Spring context
 * initialization and closed when the context is destroyed.
 */
@Service
@RequiredArgsConstructor
public class StubService {
    private final StubProperties properties;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private Cloner cloner;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private TestExecutor testExecutor;

    private final Set<String> stubNames = new HashSet<>();
    private final List<ScopedMock> scopedMocks = new ArrayList<>();

    /**
     * Registers construction and static method stubs declared in {@link JunitExtension}.
     * <p>
     * For construction stubs, the real object created by the original constructor
     * is stored inside {@link StubConstructionAnswer} to delegate calls when needed.
     *
     * @throws ClassNotFoundException if a stubbed class cannot be loaded
     */
    @PostConstruct
    public void postConstruct() throws ClassNotFoundException {
        StubAnswerBuilder builder = new StubAnswerBuilder();
        for (StubConstruction constructionStub : JunitExtension.getConstructionStubs()) {
            builder.from(constructionStub);
            MockedConstruction<?> mockedConstruction = Mockito.mockConstruction(builder.buildClass(),
                    Mockito.withSettings().defaultAnswer(new StubConstructionAnswer(builder.buildAnswer())).stubOnly(),
                    (mock, context) -> {
                        Constructor<?> constructor = context.constructor();
                        ByteBuddyUtils.makeAccessible(constructor);
                        StubConstructionAnswer ans = (StubConstructionAnswer) MockUtil.getMockSettings(mock)
                                .getDefaultAnswer();
                        ans.getMockToObject().put(mock, constructor.newInstance(context.arguments().toArray()));
                    });
            scopedMocks.add(mockedConstruction);
        }
        for (StubStaticMethods staticMethodStub : JunitExtension.getStaticMethodStubs()) {
            builder.from(staticMethodStub);
            MockedStatic<?> mockedStatic = Mockito.mockStatic(builder.buildClass(), Mockito.withSettings()
                    .defaultAnswer(builder.buildAnswer()).stubOnly());
            scopedMocks.add(mockedStatic);
        }
    }

    /**
     * Creates a Mockito stub for a Spring bean according to {@link StubBean}
     * configuration.
     *
     * @param beanStub definition of the bean stub to create
     * @param bean     original Spring bean instance that can be used as a spy target
     * @param beanName Spring bean name used as a unique stub identifier
     * @return created Mockito mock that acts as a stub (possibly a spy of the original bean)
     */
    public Object createBeanStub(StubBean beanStub, Object bean, String beanName) {
        StubAnswerBuilder builder = new StubAnswerBuilder();
        builder.from(beanStub, beanName);
        return Mockito.mock(bean.getClass(), Mockito.withSettings().defaultAnswer(builder.buildAnswer())
                .spiedInstance(bean).stubOnly());
    }

    /**
     * Closes all registered scoped mocks (construction and static) to clean up
     * Mockito state when the Spring context is destroyed.
     */
    @PreDestroy
    public void preDestroy() {
        scopedMocks.forEach(ScopedMock::close);
    }

    /**
     * Helper class that builds {@link StubAnswer} instances and resolves
     * target classes and configuration from different stub annotations.
     */
    class StubAnswerBuilder {
        private String name;
        private Class<?> stubClass;
        private String stubClassName;
        private String[] methods;
        private boolean spy;
        private boolean cloneArgsAndResult;

        /**
         * Initializes builder fields from a {@link StubBean} definition.
         *
         * @param stub     bean stub definition
         * @param beanName Spring bean name used as a stub name
         */
        public void from(StubBean stub, String beanName) {
            name = beanName;
            stubClass = stub.stubClass();
            stubClassName = stub.stubClassName();
            methods = stub.methods();
            spy = stub.spy();
            cloneArgsAndResult = stub.cloneArgsAndResult();
        }

        /**
         * Initializes builder fields from a {@link StubConstruction} definition.
         *
         * @param stub construction stub definition
         */
        public void from(StubConstruction stub) {
            name = stub.name();
            stubClass = stub.stubClass();
            stubClassName = stub.stubClassName();
            methods = stub.methods();
            spy = stub.spy();
            cloneArgsAndResult = stub.cloneArgsAndResult();
        }

        /**
         * Initializes builder fields from a {@link StubStaticMethods} definition.
         *
         * @param stub static method stub definition
         */
        public void from(StubStaticMethods stub) {
            name = stub.name();
            stubClass = stub.stubClass();
            stubClassName = stub.stubClassName();
            methods = stub.methods();
            spy = stub.spy();
            cloneArgsAndResult = stub.cloneArgsAndResult();
        }

        /**
         * Resolves the class that should be stubbed either from a direct reference
         * or from a fully qualified class name.
         *
         * @return class to be stubbed
         * @throws ClassNotFoundException if the class name cannot be resolved
         */
        public Class<?> buildClass() throws ClassNotFoundException {
            stubClass = stubClass == Null.class ? Class.forName(stubClassName) : stubClass;
            return stubClass;
        }

        /**
         * Builds a {@link StubAnswer} and registers the stub name to prevent
         * duplicate registrations.
         *
         * @return configured {@link StubAnswer} instance
         * @throws IllegalArgumentException if a stub with the same name is already registered
         */
        public StubAnswer buildAnswer() {
            name = name.isEmpty() ? stubClass.getSimpleName() : name;
            if (stubNames.contains(name)) {
                throw new IllegalArgumentException(String.format("Stub '%s' is already registered", name));
            }
            stubNames.add(name);
            Set<String> methods = this.methods.length == 0 ? null : Set.of(this.methods);
            return new StubAnswer(name, methods, properties, testExecutor, cloner, spy, cloneArgsAndResult);
        }
    }
}
