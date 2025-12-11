package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.Test;

/**
 * Interface for setting up test initialization configurations.
 * Implementations of this interface handle the conversion of test initialization
 * configurations into state objects and apply those states to tests.
 * The interface extends Comparable to allow ordering of setup operations.
 *
 * @param <T> the type of test initialization configuration
 * @param <S> the type of state object used by this setup
 */
public interface InitSetup<T extends TestInit, S extends TestInitState<S>> extends Comparable<InitSetup<?, ?>> {
    /**
     * Returns the class of the test initialization configuration that this setup handles.
     *
     * @return the class of the test initialization configuration type
     */
    Class<T> getTestCaseInitClass();

    /**
     * Returns the default state for this setup.
     * This state is used when no previous state exists or as a base for merging.
     *
     * @return the default state instance
     */
    S defaultState();

    /**
     * Converts a test initialization configuration into a state object.
     *
     * @param init the test initialization configuration to convert
     * @return a state object representing the initialization configuration
     * @throws Exception if the conversion fails
     */
    S convert(T init) throws Exception;

    /**
     * Applies the state changes to a test.
     * This method is responsible for actually applying the state to the test environment.
     *
     * @param oldState the previous state (may be null if no previous state exists)
     * @param newState the new state to apply
     * @param test     the test to apply the state to
     * @throws Exception if applying the state fails
     */
    void apply(S oldState, S newState, Test test) throws Exception;

    /**
     * Determines whether the state should be saved after application.
     * By default, states are saved.
     *
     * @return true if the state should be saved, false otherwise
     */
    default boolean saveState() {
        return true;
    }

    /**
     * Determines whether the state should be applied immediately.
     * By default, this is the inverse of saveState() - if state is not saved,
     * it should be applied immediately.
     *
     * @return true if the state should be applied immediately, false otherwise
     */
    default boolean applyImmediately() {
        return !saveState();
    }

    /**
     * Returns the order in which this setup should be executed relative to other setups.
     * Lower values are executed first. The default order is Integer.MAX_VALUE,
     * meaning setups without a specific order will be executed last.
     *
     * @return the order value for this setup
     */
    default Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * Compares this setup with another setup based on their order values.
     * This allows setups to be sorted and executed in a specific sequence.
     *
     * @param o the other setup to compare with
     * @return a negative integer, zero, or a positive integer as this setup's order
     * is less than, equal to, or greater than the other setup's order
     */
    @Override
    default int compareTo(InitSetup<?, ?> o) {
        return getOrder().compareTo(o.getOrder());
    }
}
