package io.github.dimkich.integration.testing.redis.wait;

import io.github.dimkich.integration.testing.redis.RedisKeyspaceEventsAwait;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Waits until each keyspace notification handler has been called for every notification of its
 * types observed in the current window. One instance serves all annotations targeting one
 * logical Redis connection and therefore one {@link RedisKeyspaceSubscriber}.
 * <p>
 * Completeness of the observation is proved by the barrier token that the replication wait
 * completion publishes on every stabilization step. The subscriber waits for a new token of its
 * own connection; notifications published before the token are delivered to the subscriber first.
 * Only then are the accumulated counts compared with the handler call counters. No extra PUBLISH
 * is issued by this strategy.
 */
@RequiredArgsConstructor
public class RedisKeyspaceEventsWaitCompletion implements WaitCompletion {
    private final RedisKeyspaceSubscriber subscriber;
    private final RedisProperties properties;
    private final List<RedisKeyspaceEventsAwait> awaits;

    private List<AwaitState> states = List.of();
    private String lastBarrierToken;

    @Override
    public void start() {
        List<AwaitState> newStates = new ArrayList<>(awaits.size());
        for (RedisKeyspaceEventsAwait await : awaits) {
            Set<String> eventTypes = new LinkedHashSet<>(Arrays.asList(await.events()));
            int pointcutId = KeyspaceEventTracker.pointcutId(await.pointcut());
            newStates.add(new AwaitState(pointcutId, eventTypes, await.timeoutMs(),
                    subscriber.total(eventTypes), KeyspaceEventTracker.calls(pointcutId)));
        }
        states = newStates;
        lastBarrierToken = subscriber.lastSyncToken();
    }

    @Override
    public boolean awaitAndCheckActivity() {
        // The barrier task runs in parallel and may spend its whole timeout before publishing.
        long tokenTimeout = properties.getSyncBarrierTimeoutMs() * 2L;
        lastBarrierToken = subscriber.awaitSyncTokenNot(lastBarrierToken, tokenTimeout);

        boolean activity = false;
        for (AwaitState state : states) {
            long observedNow = subscriber.total(state.eventTypes);
            long expected = observedNow - state.observed;
            if (expected > 0) {
                KeyspaceEventTracker.awaitCallsAtLeast(state.pointcutId, state.calls + expected, state.timeoutMs);
                // Only the calls of the notifications counted above are booked. Booking the raw
                // counter would also include calls for notifications published after the snapshot,
                // and the next window would wait for them twice.
                state.calls += expected;
                activity = true;
            }
            state.observed = observedNow;
        }
        return activity;
    }

    /** Baseline and configuration of one annotation. */
    private static final class AwaitState {
        private final int pointcutId;
        private final Set<String> eventTypes;
        private final long timeoutMs;
        private long observed;
        private long calls;

        private AwaitState(int pointcutId, Set<String> eventTypes, long timeoutMs,
                           long observed, long calls) {
            this.pointcutId = pointcutId;
            this.eventTypes = eventTypes;
            this.timeoutMs = timeoutMs;
            this.observed = observed;
            this.calls = calls;
        }
    }
}
