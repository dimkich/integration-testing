package io.github.dimkich.integration.testing.initialization;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.Test;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class InitStateBuilder<T extends TestInit, S extends TestInitState<S>> {
    private final InitSetup<T, S> initSetup;
    private final Deque<S> stateStack = new ArrayDeque<>();
    private final Map<T, S> initsToStatesCache = new HashMap<>();

    private final Deque<T> testContainerInits = new ArrayDeque<>();
    private final Deque<T> testCaseInits = new ArrayDeque<>();
    private final Deque<T> testPartInits = new ArrayDeque<>();
    private final List<T> testInits = new ArrayList<>();
    @Getter
    private final AddBuilder addBuilder = new AddBuilder();
    @Getter
    private final RemoveBuilder removeBuilder = new RemoveBuilder();
    private S currentState;

    public void clear() {
        stateStack.clear();
        initsToStatesCache.clear();
    }

    private Deque<T> getInits(Test.Type type) {
        return switch (type) {
            case TestContainer -> testContainerInits;
            case TestCase -> testCaseInits;
            case TestPart -> testPartInits;
        };
    }

    public class AddBuilder {
        public Test test;

        public void add(T init) {
            if (init.getApplyTo() == null) {
                testInits.add(init);
                return;
            }
            getInits(init.getApplyTo()).add(init);
        }

        public void build(Test test) throws Exception {
            Deque<T> inits = getInits(test.getType());
            boolean newInit = !inits.isEmpty() || !testInits.isEmpty();
            if (newInit) {
                if (stateStack.isEmpty()) {
                    stateStack.add(initSetup.defaultState());
                    if (currentState == null) {
                        currentState = stateStack.getLast();
                    }
                }
                S state;
                if (test.getType() == Test.Type.TestPart && !test.isFirstLeaf()) {
                    state = currentState.copy();
                } else {
                    state = stateStack.getLast().copy();
                }
                for (T init : inits) {
                    state = state.merge(initsToStatesCache.computeIfAbsent(init,
                            SneakyFunction.sneaky(initSetup::convert)));
                }
                for (T init : testInits) {
                    state = state.merge(initsToStatesCache.computeIfAbsent(init,
                            SneakyFunction.sneaky(initSetup::convert)));
                }
                stateStack.add(state);
                testInits.clear();
            }

            if (currentState != null && !stateStack.isEmpty() && currentState != stateStack.getLast()
                    && (initSetup.applyImmediately() || !test.isContainer())) {
                initSetup.apply(currentState, stateStack.getLast(), test);
                if (!initSetup.saveState()) {
                    while (stateStack.size() > 1) {
                        stateStack.removeLast();
                    }
                }
                currentState = stateStack.getLast();
            }
        }
    }

    public class RemoveBuilder {
        private boolean testInits = false;
        private int removeLast = 0;

        public void remove(T init) {
            if (init.getApplyTo() == null) {
                testInits = true;
                return;
            }
            assert init.equals(getInits(init.getApplyTo()).removeLast());
        }

        public void build(Test test) {
            if (!initSetup.saveState()) {
                return;
            }
            if (test.getType() != Test.Type.TestPart && removeLast > 0) {
                while (!stateStack.isEmpty() && removeLast > 0) {
                    stateStack.removeLast();
                    removeLast--;
                }
            }
            if (!getInits(test.getType()).isEmpty() || testInits || test.getParentTest() == null) {
                if (test.getType() == Test.Type.TestPart) {
                    removeLast++;
                } else if (!stateStack.isEmpty()) {
                    stateStack.removeLast();
                }
                testInits = false;
            }
        }
    }
}
