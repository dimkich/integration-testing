package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import io.github.sugarcubes.cloner.Cloner;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.ScopedMock;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MockService {
    private final MockInvokeProperties properties;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private Cloner cloner;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private JunitExecutable junitExecutable;

    private final Set<String> mocks = new HashSet<>();
    private final List<MockedStatic<?>> staticMocks = new ArrayList<>();
    private final List<MockedConstruction<?>> constructorMocks = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        for (TestStaticMock staticMock : JunitExtension.getStaticMocks()) {
            staticMocks.add(Mockito.mockStatic(staticMock.mockClass(), Mockito.withSettings().defaultAnswer(
                    createMockAnswer(staticMock.name(), staticMock.methods().length == 0 ? null : Set.of(staticMock.methods()),
                            staticMock.spy(), staticMock.cloneArgsAndResult())
            )));
        }
        for (TestConstructorMock constructorMock : JunitExtension.getConstructorMocks()) {
            MockedConstruction<?> mockedConstruction = Mockito.mockConstruction(constructorMock.mockClass(),
                    Mockito.withSettings().defaultAnswer(new ConstructorMockAnswer(
                            createMockAnswer(constructorMock.name(), constructorMock.methods().length == 0 ? null
                                    : Set.of(constructorMock.methods()), constructorMock.spy(),
                                    constructorMock.cloneArgsAndResult())
                    )),
                    (mock, context) -> {
                        Constructor<?> constructor = context.constructor();
                        Class<?> type = constructor.getDeclaringClass();
                        Module module = getClass().getModule();
                        if (!type.getModule().isOpen(type.getPackageName(), module)) {
                            ByteBuddyAgent.install().redefineModule(type.getModule(), Set.of(), Map.of(),
                                    Map.of(type.getPackageName(), Set.of(module)), Set.of(), Map.of());
                        }
                        constructor.setAccessible(true);
                        Object a = constructor.newInstance(context.arguments().toArray());
                        ConstructorMockAnswer ans = (ConstructorMockAnswer) MockUtil.getMockSettings(mock)
                                .getDefaultAnswer();
                        ans.setObject(a);
                    });
            constructorMocks.add(mockedConstruction);
        }
    }

    public <T> T createMock(T instance, String name, boolean isSpy, boolean cloneArgsAndResult) {
        return Mockito.mock((Class<T>) instance.getClass(), Mockito.withSettings()
                .defaultAnswer(createMockAnswer(name, null, isSpy, cloneArgsAndResult))
                .spiedInstance(instance));
    }

    @PreDestroy
    public void preDestroy() {
        staticMocks.forEach(ScopedMock::close);
        constructorMocks.forEach(ScopedMock::close);
    }

    private MockAnswer createMockAnswer(String name, Set<String> methods, boolean isSpy, boolean cloneArgsAndResult) {
        if (mocks.contains(name)) {
            throw new IllegalArgumentException(String.format("Object %s is already registered", name));
        }
        mocks.add(name);
        return new MockAnswer(name, methods, properties, junitExecutable, cloner, isSpy, cloneArgsAndResult);
    }
}
