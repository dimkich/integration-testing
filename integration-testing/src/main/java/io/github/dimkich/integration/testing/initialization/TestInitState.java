package io.github.dimkich.integration.testing.initialization;

/**
 * Represents a state object used during test initialization.
 * This interface provides methods for merging and copying state instances,
 * enabling state management and composition in test setup scenarios.
 *
 * @param <T> the type of the state implementation, which must extend TestInitState
 */
public interface TestInitState<T extends TestInitState<T>> {
    /**
     * Merges this state with another state instance.
     * The merge operation combines the properties of both states,
     * with the provided state potentially overriding values in this state.
     *
     * @param state the state to merge with this state
     * @return a new state instance that is the result of merging this state with the provided state
     */
    T merge(T state);

    /**
     * Creates a copy of this state instance.
     * The copy should be a deep copy, ensuring that modifications to the copy
     * do not affect the original state.
     *
     * @return a new state instance that is a copy of this state
     */
    T copy();
}
