package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.TestCaseInit;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InitializationService {
    private final Map<Class<? extends TestCaseInit>, Pair<TestCaseInit.Initializer<TestCaseInit>, List<TestCaseInit>>> inits;

    @SuppressWarnings("unchecked")
    public InitializationService(List<TestCaseInit.Initializer<? extends TestCaseInit>> builders) {
        this.inits = builders.stream()
                .sorted()
                .collect(Collectors.toMap(TestCaseInit.Initializer::getTestCaseInitClass,
                        v -> Pair.of((TestCaseInit.Initializer<TestCaseInit>) v, new ArrayList<>()), (v1, v2) -> v2,
                        LinkedHashMap::new));
    }

    public void addInit(TestCaseInit init) throws Exception {
        inits.get(init.getClass()).getValue().add(init);
    }

    public void init() throws Exception {
        for (Pair<TestCaseInit.Initializer<TestCaseInit>, List<TestCaseInit>> pair : inits.values()) {
            if (!pair.getValue().isEmpty()) {
                pair.getKey().init(pair.getValue());
                pair.getValue().clear();
            }
        }
    }
}
