package io.github.dimkich.integration.testing.initialization.mock;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link InitSetup} for mock initialization in integration tests.
 * This class handles the setup and execution of mock reset operations during test execution.
 *
 * <p>Mock initialization allows test configurations to reset all mocks in the current test
 * and its parent test hierarchy. This is useful for ensuring clean mock state between
 * test runs and preventing mock interactions from one test from affecting another.
 *
 * <p>This setup implementation:
 * <ul>
 *   <li>Converts {@link MockInit} configurations into {@link MockInitState} objects</li>
 *   <li>Resets all {@link MockInvoke} objects in the test hierarchy when requested</li>
 *   <li>Traverses the test hierarchy from the current test up to the root test</li>
 *   <li>Does not persist state after application ({@link #saveState()} returns {@code false})</li>
 * </ul>
 *
 * <p>When {@code resetAll} is enabled, all mocks in the current test and all parent tests
 * are reset to their initial state, ensuring a clean slate for subsequent test execution.
 *
 * @author dimkich
 * @see InitSetup
 * @see MockInit
 * @see MockInitState
 * @see MockInvoke
 */
@Slf4j
public class MockInitSetup implements InitSetup<MockInit, MockInitState> {
    /**
     * Returns the class of test initialization configuration that this setup handles.
     *
     * @return the {@link MockInit} class
     */
    @Override
    public Class<MockInit> getTestCaseInitClass() {
        return MockInit.class;
    }

    /**
     * Returns a default empty state for mock initialization.
     * This is used when no previous state exists or as a base for merging states.
     *
     * @return a new {@link MockInitState} instance with {@code resetAll} set to {@code false}
     */
    @Override
    public MockInitState defaultState() {
        return new MockInitState();
    }

    /**
     * Converts a {@link MockInit} configuration into a {@link MockInitState} object.
     * This method extracts the {@code resetAll} flag from the initialization configuration
     * and stores it in the state object.
     *
     * @param init the mock initialization configuration to convert
     * @return a state object containing the reset all flag
     */
    @Override
    public MockInitState convert(MockInit init) {
        return new MockInitState(init.getResetAll() != null && init.getResetAll());
    }

    /**
     * Applies the mock initialization state to a test by resetting all mocks when requested.
     *
     * <p>This method:
     * <ul>
     *   <li>Checks if {@code resetAll} is enabled in the new state</li>
     *   <li>If enabled, traverses the test hierarchy from the current test up to the root</li>
     *   <li>Resets all {@link MockInvoke} objects found in each test in the hierarchy</li>
     *   <li>Logs debug information when resetting mocks</li>
     * </ul>
     *
     * <p>The {@code oldState} parameter is ignored as mock resets are executed
     * independently without considering previous state.
     *
     * @param oldState the previous state (ignored in this implementation)
     * @param newState the new state containing the reset all flag
     * @param test     the test to apply the initialization to, starting point for hierarchy traversal
     * @throws Exception if resetting mocks fails
     */
    @Override
    public void apply(MockInitState oldState, MockInitState newState, Test test) throws Exception {
        if (newState.isResetAll()) {
            log.debug("Mock init reset all mocks");
            while (test != null) {
                test.getMockInvoke().forEach(MockInvoke::reset);
                test = test.getParentTest();
            }
        }
    }

    /**
     * Indicates whether the state should be persisted after application.
     *
     * <p>Returns {@code false} to indicate that mock initialization state should not be saved.
     * This means mock resets are executed immediately rather than being stored
     * for later reference.
     *
     * @return {@code false} - state is not persisted
     */
    @Override
    public boolean saveState() {
        return false;
    }
}
