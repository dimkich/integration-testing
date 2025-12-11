package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.Test;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InitializationService {
    private final Map<Class<? extends TestInit>, InitStateBuilder<TestInit, ?>> inits;

    @SuppressWarnings("unchecked")
    public InitializationService(List<InitSetup<? extends TestInit,
            ? extends TestInitState<?>>> initSetups) {
        this.inits = initSetups.stream()
                .sorted()
                .collect(Collectors.toMap(InitSetup::getTestCaseInitClass,
                        is -> new InitStateBuilder<>((InitSetup<TestInit, ?>) is),
                        (v1, v2) -> v2, LinkedHashMap::new));
    }

    public void beforeTest(Test test) throws Exception {
        for (TestInit init : test.getInits()) {
            inits.get(init.getClass()).getAddBuilder().add(init);
        }
        for (InitStateBuilder<TestInit, ?> builder : inits.values()) {
            builder.getAddBuilder().build(test);
        }
    }

    public void afterTest(Test test) {
        ListIterator<TestInit> li = test.getInits().listIterator(test.getInits().size());
        while (li.hasPrevious()) {
            TestInit init = li.previous();
            inits.get(init.getClass()).getRemoveBuilder().remove(init);
        }
        for (InitStateBuilder<TestInit, ?> builder : inits.values()) {
            builder.getRemoveBuilder().build(test);
        }
    }
}
