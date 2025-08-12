package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.ScopedMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MockService {
    private final MockInvokeProperties properties;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private JunitExecutable junitExecutable;

    private final Set<String> mocks = new HashSet<>();
    private final List<MockedStatic<?>> staticMocks = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        for (TestStaticMock staticMock : JunitExtension.getStaticMocks()) {
            staticMocks.add(Mockito.mockStatic(staticMock.mockClass(), Mockito.withSettings().defaultAnswer(
                    createMockAnswer(staticMock.name(), staticMock.methods().length == 0 ? null : Set.of(staticMock.methods()),
                            staticMock.spy())
            )));
        }
    }

    public <T> T createMock(T instance, String name, boolean isSpy) {
        return Mockito.mock((Class<T>) instance.getClass(), Mockito.withSettings()
                .defaultAnswer(createMockAnswer(name, null, isSpy))
                .spiedInstance(instance));
    }

    @PreDestroy
    public void preDestroy() {
        staticMocks.forEach(ScopedMock::close);
    }

    private MockAnswer createMockAnswer(String name, Set<String> methods, boolean isSpy) {
        if (mocks.contains(name)) {
            throw new IllegalArgumentException(String.format("Object %s is already registered", name));
        }
        mocks.add(name);
        return new MockAnswer(name, methods, properties, junitExecutable, isSpy);
    }
}
