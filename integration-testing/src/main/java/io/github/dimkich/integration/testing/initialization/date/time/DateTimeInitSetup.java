package io.github.dimkich.integration.testing.initialization.date.time;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;

/**
 * Implementation of {@link InitSetup} for date/time initialization in integration tests.
 * This class handles the setup and application of date/time configurations during test execution.
 *
 * <p>Date/time initialization allows test configurations to set a specific date/time or add
 * a duration to the current time. This is useful for testing time-dependent functionality,
 * such as scheduled tasks, time-based validations, or date calculations.
 *
 * <p>This setup implementation:
 * <ul>
 *   <li>Converts {@link DateTimeInit} configurations into {@link DateTimeInitState} objects</li>
 *   <li>Sets the current time in the {@link DateTimeService} during test setup</li>
 *   <li>Supports absolute date/time values or duration-based increments</li>
 *   <li>Runs with an execution order of 0, making it one of the first initializations to occur</li>
 * </ul>
 *
 * <p>The date/time resolution follows these rules:
 * <ul>
 *   <li>If an absolute date/time is specified, it is used as the base</li>
 *   <li>If no date/time is specified but a duration is provided, the duration is added to the current time</li>
 *   <li>If both date/time and duration are specified, the duration is added to the specified date/time</li>
 * </ul>
 *
 * @author dimkich
 * @see InitSetup
 * @see DateTimeInit
 * @see DateTimeInitState
 * @see DateTimeService
 */
@Slf4j
@RequiredArgsConstructor
public class DateTimeInitSetup implements InitSetup<DateTimeInit, DateTimeInitState> {
    /**
     * The date/time service used to set the current time for tests.
     */
    private final DateTimeService dateTimeService;

    /**
     * Returns the class of test initialization configuration that this setup handles.
     *
     * @return the {@link DateTimeInit} class
     */
    @Override
    public Class<DateTimeInit> getTestCaseInitClass() {
        return DateTimeInit.class;
    }

    /**
     * Returns a default empty state for date/time initialization.
     * This is used when no previous state exists or as a base for merging states.
     *
     * @return a new {@link DateTimeInitState} instance with both date/time and duration set to {@code null}
     */
    @Override
    public DateTimeInitState defaultState() {
        return new DateTimeInitState(null, null);
    }

    /**
     * Converts a {@link DateTimeInit} configuration into a {@link DateTimeInitState} object.
     * This method extracts the date/time and duration from the initialization configuration
     * and stores them in the state object.
     *
     * @param init the date/time initialization configuration to convert
     * @return a state object containing the date/time and duration values
     */
    @Override
    public DateTimeInitState convert(DateTimeInit init) {
        return new DateTimeInitState(init.getDateTime(), init.getAddDuration());
    }

    /**
     * Applies the date/time initialization state to a test by setting the current time
     * in the date/time service.
     *
     * <p>This method:
     * <ul>
     *   <li>Resolves the target date/time from the state configuration</li>
     *   <li>If no date/time is specified, uses the current system time as the base</li>
     *   <li>If a duration is specified, adds it to the resolved date/time</li>
     *   <li>Sets the resolved date/time in the {@link DateTimeService}</li>
     *   <li>Logs debug information about the applied date/time</li>
     * </ul>
     *
     * <p>The {@code oldState} parameter is ignored as date/time is set independently
     * without considering previous state.
     *
     * @param oldState the previous state (ignored in this implementation)
     * @param newState the new state containing date/time and duration to apply
     * @param test     the test to apply the initialization to
     * @throws Exception if setting the date/time in the service fails
     */
    @Override
    public void apply(DateTimeInitState oldState, DateTimeInitState newState, Test test) throws Exception {
        ZonedDateTime dateTime = newState.getDateTime();
        if (dateTime == null) {
            dateTime = ZonedDateTime.now();
        }
        if (newState.getAddDuration() != null) {
            dateTime = dateTime.plus(newState.getAddDuration());
        }
        log.debug("DateTime init: {}", dateTime);
        dateTimeService.setNow(dateTime);
    }

    /**
     * Returns the execution order for this setup relative to other initialization setups.
     *
     * <p>Returns 0, which places this setup at the beginning of the execution order,
     * ensuring date/time is set before other initializations that may depend on it.
     * Lower order values are executed before higher ones.
     *
     * @return the order value {@code 0}
     */
    @Override
    public Integer getOrder() {
        return 0;
    }
}
