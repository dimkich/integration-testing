package io.github.dimkich.integration.testing.redis;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Waits until a method that handles Redis keyspace notifications has been called for every
 * notification of the configured types published during the test action.
 * <p>
 * The framework subscribes to {@code __keyevent@*__:<type>} for every listed type and counts
 * the notifications actually published by Redis. The method selected by {@link #pointcut()} is
 * instrumented and its invocations are counted; the wait completes when the number of calls
 * has caught up with the number of notifications. The selected method must therefore be called
 * once for every notification of the listed types — a standard Spring Data listener satisfies
 * this even when it filters keys inside its body.
 * <p>
 * Completeness of the notification counter is proved by the token of the existing
 * {@link io.github.dimkich.integration.testing.redis.replication.RedisSyncBarrier}: messages
 * published before the token are received by the subscriber before the token itself.
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RedisKeyspaceEventsAwait.List.class)
public @interface RedisKeyspaceEventsAwait {

    /** Keyspace event types to wait for, as used in {@code __keyevent@*__:<type>} channels. */
    String[] events();

    /**
     * Pointcut selecting the notification handler method, for example
     * {@code "t.name('...Listener') && m.name('onMessage')"}.
     */
    String pointcut();

    /**
     * Logical Redis connection to subscribe on. May be empty only when exactly one connection
     * is configured under {@code integration.testing.redis.connections}; otherwise the name is
     * required. Annotations targeting the same connection share one subscriber.
     */
    String connection() default "";

    /** Maximum time to wait for a handler call, in milliseconds. */
    long timeoutMs() default 5000;

    /** Container annotation for repeated declarations. */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        RedisKeyspaceEventsAwait[] value();
    }
}
