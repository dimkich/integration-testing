package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestInit;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InitializationService {
    private final Map<Class<? extends TestInit>, InitState> inits;

    @SuppressWarnings("unchecked")
    public InitializationService(List<TestInit.Initializer<? extends TestInit>> builders) {
        this.inits = builders.stream()
                .sorted()
                .collect(Collectors.toMap(TestInit.Initializer::getTestInitClass,
                        v -> new InitState((TestInit.Initializer<TestInit>) v), (v1, v2) -> v2,
                        LinkedHashMap::new));
    }

    public void beforeTest(Test test) throws Exception {
        for (TestInit init : test.getInits()) {
            inits.get(init.getClass()).add(init);
        }
        for (InitState initState : inits.values()) {
            initState.init(test.getType());
        }
    }

    public void afterTest(Test test) {
        ListIterator<TestInit> li = test.getInits().listIterator(test.getInits().size());
        while (li.hasPrevious()) {
            TestInit init = li.previous();
            inits.get(init.getClass()).remove(init);
        }
    }
}
