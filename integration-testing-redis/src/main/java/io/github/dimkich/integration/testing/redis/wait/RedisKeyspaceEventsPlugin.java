package io.github.dimkich.integration.testing.redis.wait;

import io.github.dimkich.integration.testing.expression.ExpressionFactory;
import io.github.dimkich.integration.testing.expression.PointcutMatch;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import io.github.dimkich.integration.testing.redis.RedisKeyspaceEventsAwait;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedRepeatableAnnotations;

/**
 * Instruments notification handlers declared by {@link RedisKeyspaceEventsAwait}.
 * <p>
 * The discovered declarations are kept statically so that {@code RedisConfig.PostProcessor},
 * which runs when the Spring context is created after instrumentation, can register the
 * subscriber and wait-completion beans for the connections they target.
 */
public class RedisKeyspaceEventsPlugin implements InstrumentationPlugin {
    private static final List<RedisKeyspaceEventsAwait> awaits = new ArrayList<>();

    @Override
    public boolean isApplicable(Class<?> testClass) {
        awaits.clear();
        awaits.addAll(findMergedRepeatableAnnotations(testClass, RedisKeyspaceEventsAwait.class));
        return !awaits.isEmpty();
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) {
        Set<String> instrumentedPointcuts = new HashSet<>();
        for (RedisKeyspaceEventsAwait await : awaits) {
            if (!instrumentedPointcuts.add(await.pointcut())) {
                continue;
            }
            PointcutMatch match = ExpressionFactory.createPointcutMatch(await.pointcut());
            KeyspaceEventTracker.registerPointcut(await.pointcut(), match.getPointcutId());
            builder = match.apply(builder, KeyspaceEventAdvice.class);
        }
        return builder;
    }

    @Override
    public void cleanup() {
        awaits.clear();
        KeyspaceEventTracker.reset();
    }

    /** Validates the connection names declared by the annotations against the configured ones. */
    public static void validateConnections(Set<String> connectionNames) {
        for (RedisKeyspaceEventsAwait await : awaits) {
            if (await.connection().isEmpty()) {
                if (connectionNames.size() != 1) {
                    throw new IllegalStateException("Specify connection in @RedisKeyspaceEventsAwait: "
                            + "multiple Redis connections are configured");
                }
            } else if (!connectionNames.contains(await.connection())) {
                throw new IllegalStateException("Redis connection '" + await.connection()
                        + "' from @RedisKeyspaceEventsAwait is not configured");
            }
        }
    }

    /** Declarations targeting the given connection. */
    public static List<RedisKeyspaceEventsAwait> annotationsFor(String connectionName, boolean singleConnection) {
        return awaits.stream()
                .filter(await -> await.connection().isEmpty()
                        ? singleConnection
                        : await.connection().equals(connectionName))
                .toList();
    }
}
