package io.github.dimkich.integration.testing.initialization;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestContainer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class InitStateBuilder<T extends TestInit, S extends TestInitState<S>> {
    private final static TestContainer TEST = new TestContainer();

    private final InitSetup<T, S> initSetup;
    private final Deque<S> stateStack = new ArrayDeque<>();
    private final Deque<Test> testStack = new ArrayDeque<>();
    private final Map<T, S> initsToStatesCache = new HashMap<>();

    private final CursorStack<T> testContainerInits = new CursorStack<>();
    private final CursorStack<T> testCaseInits = new CursorStack<>();
    private final CursorStack<T> testPartInits = new CursorStack<>();
    private final SegmentedList<T, Test> testInits = new SegmentedList<>();
    @Getter
    private final AddBuilder addBuilder = new AddBuilder();
    @Getter
    private final RemoveBuilder removeBuilder = new RemoveBuilder();
    private S currentState;

    public void clear() {
        stateStack.clear();
        testStack.clear();
        initsToStatesCache.clear();
    }

    public S changeCurrentStatus() {
        if (!stateStack.isEmpty() && currentState == stateStack.getLast()) {
            currentState = currentState.copy();
        }
        return currentState;
    }

    private CursorStack<T> getInits(Test.Type type) {
        return switch (type) {
            case TestContainer -> testContainerInits;
            case TestCase -> testCaseInits;
            case TestPart -> testPartInits;
        };
    }

    public class AddBuilder {
        public void add(Test test) {
            testInits.addAll(getInits(test.getType()).readFromCursor());
        }

        public void add(T init) {
            if (init.getApplyTo() == null) {
                testInits.add(init);
                return;
            }
            getInits(init.getApplyTo()).push(init);
        }

        public void build(Test test) throws Exception {
            testInits.finishSegment(test);
            if (test.isContainer()) {
                return;
            }
            if (!testInits.isEmpty()) {
                if (stateStack.isEmpty()) {
                    stateStack.add(initSetup.defaultState());
                    testStack.add(TEST);
                    if (currentState == null) {
                        currentState = stateStack.getLast();
                    }
                    log.debug("init state initialized, current state {}", currentState);
                }

                if (initSetup.saveState()) {
                    S state = stateStack.getLast();
                    if (test.getType() == Test.Type.TestPart && !test.isFirstLeaf()) {
                        state = currentState;
                    }
                    for (int i = 0; i < testInits.getSegmentCount(); i++) {
                        List<T> inits = testInits.getSegment(i);
                        if (!inits.isEmpty()) {
                            state = state.copy();
                            for (T init : inits) {
                                state = state.merge(initsToStatesCache.computeIfAbsent(init,
                                        SneakyFunction.sneaky(initSetup::convert)));
                            }
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("init state added");
                            log.debug("test = {}", testInits.getSegmentData(i).getName());
                            log.debug("state = {}", state);
                        }
                        stateStack.add(state);
                        testStack.add(testInits.getSegmentData(i));
                    }
                } else {
                    S state = stateStack.getLast().copy();
                    for (T init : testInits.getElements()) {
                        state = state.merge(initsToStatesCache.computeIfAbsent(init,
                                SneakyFunction.sneaky(initSetup::convert)));
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("init state added");
                        log.debug("test = {}", test.getName());
                        log.debug("state = {}", state);
                    }
                    stateStack.add(state);
                    testStack.add(test);
                }
                testInits.clear();
                testContainerInits.resetCursor();
                testCaseInits.resetCursor();
                testPartInits.resetCursor();
            } else if (test.getType() == Test.Type.TestPart && !test.isFirstLeaf()) {
                return;
            }

            if (currentState != null && !stateStack.isEmpty() && currentState != stateStack.getLast()) {
                initSetup.apply(currentState, stateStack.getLast(), test);
                currentState = stateStack.getLast();
                log.debug("init state applied, current state {}", currentState);
            }
        }
    }

    public class RemoveBuilder {
        public void remove(T init) {
            if (init.getApplyTo() == null) {
                return;
            }
            assert init.equals(getInits(init.getApplyTo()).pop());
        }

        public void build(Test test) {
            if (!testStack.isEmpty() && test == testStack.getLast()) {
                S state = stateStack.removeLast();
                log.debug("init state removed {}", state);
                testStack.removeLast();
            }
        }
    }
}
