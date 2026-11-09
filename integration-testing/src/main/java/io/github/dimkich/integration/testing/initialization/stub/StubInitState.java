package io.github.dimkich.integration.testing.initialization.stub;

import io.github.dimkich.integration.testing.execution.StubCall;
import io.github.dimkich.integration.testing.initialization.TestInitState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the initialization state for stub reset operations in integration tests.
 *
 * <p>This state object tracks whether all stubs should be reset during test initialization.
 * It implements the {@link TestInitState} interface to support state merging and copying
 * operations required for test initialization composition.
 *
 * <p>The state contains a single boolean flag {@code resetAll} that indicates whether
 * all stubs in the test hierarchy should be reset. When multiple initialization configurations
 * are merged, if any configuration requests resetting all stubs, the final state will
 * have {@code resetAll} set to {@code true}.
 *
 * <p>This class is typically used in conjunction with {@link StubInit} configurations
 * and processed by {@link StubInitSetup} during test execution.
 *
 * @see TestInitState
 * @see StubInit
 * @see StubInitSetup
 * @see StubCall
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StubInitState implements TestInitState<StubInitState> {

    /**
     * Indicates whether all stubs should be reset in the current test and its parent hierarchy.
     * When {@code true}, all {@link StubCall} objects in the test hierarchy will be reset
     * to their initial state during test initialization.
     *
     * <p>This flag follows a "maximum" merge strategy - if any merged state has this flag
     * set to {@code true}, the resulting state will also have it set to {@code true}.
     */
    private boolean resetAll;

    /**
     * Merges this state with another {@code StubInitState} instance.
     *
     * <p>The merge operation follows these rules:
     * <ul>
     *   <li>If the provided state has {@code resetAll} set to {@code true}, this state's
     *       {@code resetAll} flag will be set to {@code true}</li>
     *   <li>If the provided state has {@code resetAll} set to {@code false}, this state's
     *       {@code resetAll} flag remains unchanged</li>
     *   <li>The operation modifies and returns this instance (fluent interface pattern)</li>
     * </ul>
     *
     * <p>This merge strategy ensures that if any initialization configuration in the
     * test hierarchy requests a full stub reset, the operation will be performed.
     *
     * @param state the state to merge with this state (must not be null)
     * @return this state instance after merging, for method chaining
     * @see TestInitState#merge(TestInitState)
     */
    @Override
    public StubInitState merge(StubInitState state) {
        if (state.resetAll) {
            resetAll = true;
        }
        return this;
    }

    /**
     * Creates a copy of this state instance.
     *
     * <p>The copy is a deep copy since {@code StubInitState} only contains primitive
     * fields. The returned instance will have the same {@code resetAll} value as this instance.
     *
     * @return a new {@code StubInitState} instance with the same {@code resetAll} value
     * @see TestInitState#copy()
     */
    @Override
    public StubInitState copy() {
        return new StubInitState(resetAll);
    }
}
