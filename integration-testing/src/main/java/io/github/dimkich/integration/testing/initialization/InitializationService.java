package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.Test;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InitializationService {
    private final Map<Class<? extends TestInit>, InitStateBuilder<TestInit, ?>> inits;
    private final List<TestInit> transientInits = new ArrayList<>();

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
        for (InitStateBuilder<TestInit, ?> builder : inits.values()) {
            builder.getAddBuilder().add(test);
        }
        for (TestInit init : test.getInits()) {
            inits.get(init.getClass()).getAddBuilder().add(init);
        }
        for (InitStateBuilder<TestInit, ?> builder : inits.values()) {
            builder.getAddBuilder().build(test);
        }
    }

    public void addTransientInit(TestInit init) {
        transientInits.add(init);
        inits.get(init.getClass()).getAddBuilder().add(init);
    }

    public void afterTest(Test test) {
        removeInits(transientInits);
        removeInits(test.getInits());
        for (InitStateBuilder<TestInit, ?> builder : inits.values()) {
            builder.getRemoveBuilder().build(test);
        }
    }

    private void removeInits(List<TestInit> list) {
        ListIterator<TestInit> li = list.listIterator(list.size());
        while (li.hasPrevious()) {
            TestInit init = li.previous();
            inits.get(init.getClass()).getRemoveBuilder().remove(init);
        }
    }
}
