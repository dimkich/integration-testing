package io.github.dimkich.integration.testing.initialization;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestContainer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Builds the stack of init states while a test tree is being executed: collects {@code init}
 * declarations by {@code applyTo} level, converts and merges them into {@link TestInitState}s and
 * applies state changes between tests.
 *
 * <p>{@link AddBuilder} is used when descending into a test and {@link RemoveBuilder} when leaving
 * it. When {@link InitSetup#saveState()} is enabled, every test element gets its own state, so
 * states can be applied incrementally.
 *
 * @param <T> init declaration type
 * @param <S> init state type
 */
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

    /**
     * Clears the accumulated state and test stacks.
     */
    public void clear() {
        stateStack.clear();
        testStack.clear();
        initsToStatesCache.clear();
    }

    /**
     * Marks the current state as modified: the next change creates a copy instead of mutating the
     * state shared with the stack.
     *
     * @return the current (possibly copied) state
     */
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

    /**
     * Collects init declarations while the builder descends into the test tree.
     */
    public class AddBuilder {
        /**
         * Adds all inits declared for the given test element.
         *
         * @param test test element being entered
         */
        public void add(Test test) {
            testInits.addAll(getInits(test.getType()).readFromCursor());
        }

        /**
         * Adds a single init declaration: an init with {@code applyTo} is deferred in the cursor of
         * the corresponding level, otherwise it is added directly.
         *
         * @param init init declaration
         */
        public void add(T init) {
            if (init.getApplyTo() == null) {
                testInits.add(init);
                return;
            }
            getInits(init.getApplyTo()).push(init);
        }

        /**
         * Converts the collected inits into states and pushes them onto the stacks; applies the
         * resulting state to the setup when it differs from the current one.
         *
         * @param test test element the current segment belongs to
         * @throws Exception if the init setup fails
         */
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
                try {
                    initSetup.apply(currentState, stateStack.getLast(), test);
                } finally {
                    currentState = stateStack.getLast();
                }
                log.debug("init state applied, current state {}", currentState);
            }
        }
    }

    /**
     * Removes init declarations and states while the builder leaves the test tree.
     */
    public class RemoveBuilder {
        /**
         * Removes an init declaration with {@code applyTo} from the cursor of its level.
         *
         * @param init init declaration to remove
         */
        public void remove(T init) {
            if (init.getApplyTo() == null) {
                return;
            }
            assert init.equals(getInits(init.getApplyTo()).pop());
        }

        /**
         * Pops the state and the test element pushed when entering the given test.
         *
         * @param test test element being left
         */
        public void build(Test test) {
            if (!testStack.isEmpty() && test == testStack.getLast()) {
                S state = stateStack.removeLast();
                log.debug("init state removed {}", state);
                testStack.removeLast();
            }
        }
    }
}
