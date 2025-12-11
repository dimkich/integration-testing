package io.github.dimkich.integration.testing.initialization.bean;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents the state of bean initialization for integration tests.
 * This class maintains a collection of bean methods that need to be invoked
 * during test initialization, and provides operations for merging and copying state.
 *
 * <p>Bean initialization state is used to track which beans and methods should be
 * executed before running tests. The state can be merged from multiple initialization
 * configurations and copied for state management purposes.
 *
 * <p>The beans are stored in a {@link LinkedHashSet} to maintain insertion order
 * while ensuring uniqueness of bean method configurations.
 *
 * @author dimkich
 * @see TestInitState
 * @see BeanInit
 * @see BeanInit.BeanMethod
 * @see BeanInitSetup
 */
@Getter
public class BeanInitState implements TestInitState<BeanInitState> {
    /**
     * Set of bean methods to be invoked during test initialization.
     * Each element represents a bean and the method to call on it.
     * The set maintains insertion order and ensures uniqueness of bean method configurations.
     */
    private final Set<BeanInit.BeanMethod> beans = new LinkedHashSet<>();

    /**
     * Merges this state with another state instance.
     * All bean methods from the provided state are added to this state's collection.
     * Duplicate bean methods (based on set semantics) are automatically handled by the underlying set.
     *
     * @param state the state to merge with this state (must not be null)
     * @return this state instance after merging, for method chaining
     */
    @Override
    public BeanInitState merge(BeanInitState state) {
        beans.addAll(state.beans);
        return this;
    }

    /**
     * Creates a deep copy of this state instance.
     * The copy contains all the same bean methods as the original state,
     * but modifications to the copy will not affect the original state.
     *
     * @return a new {@link BeanInitState} instance that is a copy of this state
     */
    @Override
    public BeanInitState copy() {
        BeanInitState state = new BeanInitState();
        state.beans.addAll(beans);
        return state;
    }
}
