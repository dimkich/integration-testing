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

    public Object createBeanMock(TestBeanMock beanMock, Object bean, String beanName) {
        MockAnswerBuilder builder = new MockAnswerBuilder();
        builder.from(beanMock, beanName);
        return Mockito.mock(bean.getClass(), Mockito.withSettings().defaultAnswer(builder.buildAnswer())
                .spiedInstance(bean).stubOnly());
    }

    @PreDestroy
    public void preDestroy() {
        scopedMocks.forEach(ScopedMock::close);
    }

    class MockAnswerBuilder {
        private String name;
        private Class<?> mockClass;
        private String mockClassName;
        private String[] methods;
        private boolean spy;
        private boolean cloneArgsAndResult;

        public void from(TestBeanMock mock, String beanName) {
            name = beanName;
            mockClass = mock.mockClass();
            mockClassName = mock.mockClassName();
            methods = mock.methods();
            spy = mock.spy();
            cloneArgsAndResult = mock.cloneArgsAndResult();
        }

        public void from(TestConstructorMock mock) {
            name = mock.name();
            mockClass = mock.mockClass();
            mockClassName = mock.mockClassName();
            methods = mock.methods();
            spy = mock.spy();
            cloneArgsAndResult = mock.cloneArgsAndResult();
        }

        public void from(TestStaticMock mock) {
            name = mock.name();
            mockClass = mock.mockClass();
            mockClassName = mock.mockClassName();
            methods = mock.methods();
            spy = mock.spy();
            cloneArgsAndResult = mock.cloneArgsAndResult();
        }

        public Class<?> buildClass() throws ClassNotFoundException {
            mockClass = mockClass == Null.class ? Class.forName(mockClassName) : mockClass;
            return mockClass;
        }

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
