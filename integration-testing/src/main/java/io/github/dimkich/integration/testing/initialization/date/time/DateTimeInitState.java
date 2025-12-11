package io.github.dimkich.integration.testing.initialization.date.time;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Represents the state of date/time initialization for integration tests.
 * This class maintains an absolute date/time value and/or a duration to add,
 * providing operations for merging and copying state instances.
 *
 * <p>Date/time initialization state is used to track date/time configurations
 * that should be applied during test initialization. The state can be merged
 * from multiple initialization configurations and copied for state management purposes.
 *
 * <p>The merge operation follows these rules:
 * <ul>
 *   <li>If the merged state contains a non-null {@code dateTime}, it overrides
 *       the current date/time and clears any existing {@code addDuration}</li>
 *   <li>If the merged state contains a non-null {@code addDuration}, it is added
 *       to the existing duration (if present) or replaces it (if null)</li>
 * </ul>
 *
 * @author dimkich
 * @see TestInitState
 * @see DateTimeInit
 * @see DateTimeInitSetup
 */
@Getter
@AllArgsConstructor
public class DateTimeInitState implements TestInitState<DateTimeInitState> {
    /**
     * The absolute date/time to set.
     * If specified, this will be the base time used. If {@code addDuration} is also specified,
     * the duration will be added to this date/time. If {@code null} and {@code addDuration}
     * is provided, the duration will be added to the current time.
     */
    private ZonedDateTime dateTime;

    /**
     * Duration to add to the current or specified date/time.
     * If {@code dateTime} is specified, the duration will be added to that date/time.
     * If {@code dateTime} is {@code null}, the duration will be added to the current time.
     * During merge operations, if both states have durations, they are combined additively.
     */
    private Duration addDuration;

    /**
     * Merges this state with another state instance.
     *
     * <p>The merge operation follows these rules:
     * <ul>
     *   <li>If the provided state has a non-null {@code dateTime}, it overrides
     *       this state's date/time and clears any existing {@code addDuration}</li>
     *   <li>If the provided state has a non-null {@code addDuration}, it is combined
     *       with this state's duration: if this state already has a duration, they are
     *       added together; otherwise, the provided duration replaces this state's duration</li>
     * </ul>
     *
     * @param state the state to merge with this state (must not be null)
     * @return this state instance after merging, for method chaining
     */
    @Override
    public DateTimeInitState merge(DateTimeInitState state) {
        if (state.dateTime != null) {
            dateTime = state.dateTime;
            addDuration = null;
        }
        if (state.addDuration != null) {
            if (addDuration != null) {
                addDuration = addDuration.plus(state.addDuration);
            } else {
                addDuration = state.addDuration;
            }
        }
        return this;
    }

    /**
     * Creates a copy of this state instance.
     * The copy contains the same date/time and duration values as the original state,
     * but modifications to the copy will not affect the original state.
     *
     * @return a new {@link DateTimeInitState} instance that is a copy of this state
     */
    @Override
    public DateTimeInitState copy() {
        return new DateTimeInitState(dateTime, addDuration);
    }
}
