package io.github.dimkich.integration.testing.initialization.bean;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.MockAnswer;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;

/**
 * Implementation of {@link InitSetup} for bean-based initialization in integration tests.
 * This class handles the setup and execution of bean initialization methods during test execution.
 *
 * <p>Bean initialization allows test configurations to invoke specific methods on Spring beans
 * before test execution. This is useful for setting up test data, configuring bean state,
 * or performing any necessary pre-test initialization.
 *
 * <p>This setup implementation:
 * <ul>
 *   <li>Converts {@link BeanInit} configurations into {@link BeanInitState} objects</li>
 *   <li>Executes bean initialization methods via reflection during test setup</li>
 *   <li>Enables mock answer interception during bean initialization to capture mock interactions</li>
 *   <li>Runs with an execution order of 10000, allowing other initializations to occur first</li>
 * </ul>
 *
 * <p>Note: State is not persisted after application ({@link #saveState()} returns {@code false}),
 * meaning bean initializations are executed immediately rather than being stored for later use.
 *
 * @author dimkich
 * @see InitSetup
 * @see BeanInit
 * @see BeanInitState
 * @see MockAnswer
 */
@Slf4j
@RequiredArgsConstructor
public class BeanInitSetup implements InitSetup<BeanInit, BeanInitState> {
    /**
     * The Spring bean factory used to retrieve beans by name.
     */
    private final BeanFactory beanFactory;

    /**
     * Returns the class of test initialization configuration that this setup handles.
     *
     * @return the {@link BeanInit} class
     */
    @Override
    public Class<BeanInit> getTestCaseInitClass() {
        return BeanInit.class;
    }

    /**
     * Converts a {@link BeanInit} configuration into a {@link BeanInitState} object.
     * This method extracts the list of bean methods from the initialization configuration
     * and stores them in the state object.
     *
     * @param init the bean initialization configuration to convert
     * @return a state object containing all bean methods to be executed
     */
    @Override
    public BeanInitState convert(BeanInit init) {
        BeanInitState state = new BeanInitState();
        state.getBeans().addAll(init.getBean());
        return state;
    }

    /**
     * Returns a default empty state for bean initialization.
     * This is used when no previous state exists or as a base for merging states.
     *
     * @return a new empty {@link BeanInitState} instance
     */
    @Override
    public BeanInitState defaultState() {
        return new BeanInitState();
    }

    /**
     * Applies the bean initialization state to a test by executing all configured bean methods.
     *
     * <p>This method:
     * <ul>
     *   <li>Enables mock answer interception during initialization to capture any mock interactions</li>
     *   <li>Retrieves each configured bean from the Spring bean factory</li>
     *   <li>Invokes the specified method on each bean using reflection</li>
     *   <li>Logs debug information for each bean initialization</li>
     * </ul>
     *
     * <p>The {@code oldState} parameter is ignored as bean initializations are executed
     * independently without considering previous state.
     *
     * @param oldState the previous state (ignored in this implementation)
     * @param newState the new state containing bean methods to execute
     * @param test     the test to apply the initialization to
     * @throws Exception        if bean retrieval fails, if the method cannot be found, or if method invocation fails
     * @throws RuntimeException if a bean or method does not exist
     */
    @Override
    public void apply(BeanInitState oldState, BeanInitState newState, Test test) throws Exception {
        MockAnswer.enable(() -> {
            for (BeanInit.BeanMethod beanMethod : newState.getBeans()) {
                log.debug("Bean init {}", beanMethod);
                Object bean = beanFactory.getBean(beanMethod.getName());
                bean.getClass().getMethod(beanMethod.getMethod()).invoke(bean);
            }
        });
    }

    /**
     * Indicates whether the state should be persisted after application.
     *
     * <p>Returns {@code false} to indicate that bean initialization state should not be saved.
     * This means bean initializations are executed immediately rather than being stored
     * for later reference.
     *
     * @return {@code false} - state is not persisted
     */
    @Override
    public boolean saveState() {
        return false;
    }

    /**
     * Returns the execution order for this setup relative to other initialization setups.
     *
     * <p>Returns 10000, which places this setup relatively late in the execution order,
     * allowing other initializations (such as data storage initialization) to occur first.
     * Lower order values are executed before higher ones.
     *
     * @return the order value {@code 10000}
     */
    @Override
    public Integer getOrder() {
        return 10000;
    }
}
