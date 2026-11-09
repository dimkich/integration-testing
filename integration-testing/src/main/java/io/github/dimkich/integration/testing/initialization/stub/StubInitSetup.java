package io.github.dimkich.integration.testing.initialization.stub;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.StubCall;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link InitSetup} for stub initialization in integration tests.
 * This class handles the setup and execution of stub reset operations during test execution.
 *
 * <p>Stub initialization allows test configurations to reset all stubs in the current test
 * and its parent test hierarchy. This is useful for ensuring clean stub state between
 * test runs and preventing stub interactions from one test from affecting another.
 *
 * <p>This setup implementation:
 * <ul>
 *   <li>Converts {@link StubInit} configurations into {@link StubInitState} objects</li>
 *   <li>Resets all {@link StubCall} objects in the test hierarchy when requested</li>
 *   <li>Traverses the test hierarchy from the current test up to the root test</li>
 *   <li>Does not persist state after application ({@link #saveState()} returns {@code false})</li>
 * </ul>
 *
 * <p>When {@code resetAll} is enabled, all stubs in the current test and all parent tests
 * are reset to their initial state, ensuring a clean slate for subsequent test execution.
 *
 * @author dimkich
 * @see InitSetup
 * @see StubInit
 * @see StubInitState
 * @see StubCall
 */
@Slf4j
public class StubInitSetup implements InitSetup<StubInit, StubInitState> {
    /**
     * Returns the class of test initialization configuration that this setup handles.
     *
     * @return the {@link StubInit} class
     */
    @Override
    public Class<StubInit> getTestCaseInitClass() {
        return StubInit.class;
    }

    /**
     * Returns a default empty state for stub initialization.
     * This is used when no previous state exists or as a base for merging states.
     *
     * @return a new {@link StubInitState} instance with {@code resetAll} set to {@code false}
     */
    @Override
    public StubInitState defaultState() {
        return new StubInitState();
    }

    /**
     * Converts a {@link StubInit} configuration into a {@link StubInitState} object.
     * This method extracts the {@code resetAll} flag from the initialization configuration
     * and stores it in the state object.
     *
     * @param init the stub initialization configuration to convert
     * @return a state object containing the reset all flag
     */
    @Override
    public StubInitState convert(StubInit init) {
        return new StubInitState(init.getResetAll() != null && init.getResetAll());
    }

    /**
     * Applies the stub initialization state to a test by resetting all stubs when requested.
     *
     * <p>This method:
     * <ul>
     *   <li>Checks if {@code resetAll} is enabled in the new state</li>
     *   <li>If enabled, traverses the test hierarchy from the current test up to the root</li>
     *   <li>Resets all {@link StubCall} objects found in each test in the hierarchy</li>
     *   <li>Logs debug information when resetting stubs</li>
     * </ul>
     *
     * <p>The {@code oldState} parameter is ignored as stub resets are executed
     * independently without considering previous state.
     *
     * @param oldState the previous state (ignored in this implementation)
     * @param newState the new state containing the reset all flag
     * @param test     the test to apply the initialization to, starting point for hierarchy traversal
     */
    @Override
    public void apply(StubInitState oldState, StubInitState newState, Test test) {
        if (newState.isResetAll()) {
            log.debug("Stub init reset all stubs");
            while (test != null) {
                test.getStubCall().forEach(StubCall::reset);
                test = test.getParentTest();
            }
        }
    }

    /**
     * Indicates whether the state should be persisted after application.
     *
     * <p>Returns {@code false} to indicate that stub initialization state should not be saved.
     * This means stub resets are executed immediately rather than being stored
     * for later reference.
     *
     * @return {@code false} - state is not persisted
     */
    @Override
    public boolean saveState() {
        return false;
    }
}
