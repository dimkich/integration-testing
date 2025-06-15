package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.TestCaseInit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InitializationService {
    private final Map<Class<? extends TestCaseInit>, TestCaseInit.TestCaseInitializer<? extends TestCaseInit>> map;

    public InitializationService(List<TestCaseInit.TestCaseInitializer<? extends TestCaseInit>> initializers) {
        map = initializers.stream()
                .collect(Collectors.toMap(TestCaseInit.TestCaseInitializer::getTestCaseInitClass, Function.identity()));
    }

    @SuppressWarnings("unchecked")
    public void init(TestCaseInit init) throws Exception {
        TestCaseInit.TestCaseInitializer<TestCaseInit> initializer = (TestCaseInit.TestCaseInitializer<TestCaseInit>) map.get(init.getClass());
        if (initializer == null) {
            throw new RuntimeException(String.format("Initializer for class %s not found", init.getClass().getName()));
        }
        initializer.init(init);
    }
}
