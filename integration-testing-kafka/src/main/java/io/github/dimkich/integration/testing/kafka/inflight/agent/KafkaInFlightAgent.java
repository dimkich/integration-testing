package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.instrumentation.InstrumentationManager;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import io.github.dimkich.integration.testing.kafka.EnableTestKafka;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Byte Buddy plugin that instruments {@code KafkaProducer} and {@code KafkaConsumer}
 * to feed the in-flight ledger used for Kafka lag tracking.
 *
 * <p>The agent is applicable exactly when {@link EnableTestKafka} is present on the
 * test class and its {@code inflight} attribute is {@code true}. With
 * {@code inflight=false} (and for non-Kafka tests) it is not applied at all, so Kafka
 * clients outside inflight-enabled tests are never instrumented.
 */
public class KafkaInFlightAgent implements InstrumentationPlugin {

    /**
     * Applies this agent only when {@link EnableTestKafka} is present and its
     * {@code inflight} attribute is {@code true}.
     *
     * <p>Pure predicate with no side effects: whether inflight tracking is active is
     * derived from the agent's presence in
     * {@link InstrumentationManager#getActivePlugins()} (see {@link #isInflightActive()}),
     * not from state written here.
     */
    @Override
    public boolean isApplicable(Class<?> testClass) {
        EnableTestKafka annotation = testClass.getAnnotation(EnableTestKafka.class);
        return annotation != null && annotation.inflight();
    }

    /**
     * Whether inflight lag tracking is active for the current test. The agent is
     * registered in {@link InstrumentationManager} exactly when it is applicable
     * (annotation present and {@code inflight=true}), so this serves as the single
     * source of truth for Kafka's configuration instead of a separate mutable flag.
     */
    public static boolean isInflightActive() {
        return InstrumentationManager.getActivePlugins().stream()
                .anyMatch(plugin -> plugin instanceof KafkaInFlightAgent);
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) {
        return builder
                .type(named("org.apache.kafka.clients.producer.KafkaProducer"))
                .transform((b, td, cl, m, loaded) -> b
                        .visit(Advice.to(KafkaProducerConstructorAdvice.class)
                                .on(isConstructor().and(isPublic())))
                        .visit(Advice.to(KafkaTransactionCommitAdvice.class)
                                .on(named("commitTransaction")))
                        .visit(Advice.to(KafkaTransactionAbortAdvice.class)
                                .on(named("abortTransaction")))
                        .visit(Advice.to(KafkaSendOffsetsToTransactionAdvice.class)
                                .on(named("sendOffsetsToTransaction"))))
                .type(named("org.apache.kafka.clients.consumer.KafkaConsumer"))
                .transform((b, td, cl, m, loaded) -> b
                        .visit(Advice.to(KafkaConsumerConstructorAdvice.class)
                                .on(isConstructor().and(isPublic())))
                        // KafkaConsumer.close() (no-arg) delegates internally to close(Duration), so
                        // instrumenting only close(Duration) covers every public shutdown path without
                        // double-firing. There is no close(long, TimeUnit) overload anymore - it was
                        // deprecated and removed in Kafka 3.x. Matching named("close") would instrument
                        // both close() and close(Duration), decrementing the consumer count twice on the
                        // common no-arg path and prematurely dropping still-alive groups.
                        .visit(Advice.to(KafkaConsumerCloseAdvice.class)
                                .on(named("close").and(takesArgument(0, named("java.time.Duration")))))
                        // Only the two-argument overloads are matched: the no-argument
                        // overloads delegate to them internally, so the advice still runs
                        // for subscribe(Collection) / subscribe(Pattern) while a single
                        // matcher covers both.
                        .visit(Advice.to(KafkaConsumerSubscribeExactTopicsAdvice.class)
                                .on(named("subscribe").and(takesArguments(2))
                                        .and(takesArgument(0, named("java.util.Collection")))))
                        .visit(Advice.to(KafkaConsumerSubscribePatternAdvice.class)
                                .on(named("subscribe").and(takesArguments(2))
                                        .and(takesArgument(0, named("java.util.regex.Pattern")))))
                        .visit(Advice.to(KafkaConsumerAssignAdvice.class)
                                .on(named("assign").and(takesArgument(0, named("java.util.Collection")))))
                        .visit(Advice.to(KafkaConsumerUnsubscribeAdvice.class)
                                .on(named("unsubscribe").and(takesArguments(0)))));
    }
}
