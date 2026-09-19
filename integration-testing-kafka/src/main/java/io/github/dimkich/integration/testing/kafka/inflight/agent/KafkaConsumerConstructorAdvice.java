package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * Byte Buddy advice on {@code KafkaConsumer} constructors: registers the in-flight
 * interceptor in the config argument and registers the constructed consumer in the
 * ledger.
 */
public class KafkaConsumerConstructorAdvice {

    /**
     * Replaces the config argument with an interceptor-augmented copy.
     *
     * <p>Uses {@code @Advice.Argument(value = 0, readOnly = false)} rather than
     * {@code @Advice.AllArguments}: only the former allows the advice to write
     * back a value that the instrumented constructor actually sees. The original
     * config object is never touched — see
     * {@link io.github.dimkich.integration.testing.kafka.inflight.ledger.InterceptorInjector}
     * for the non-mutating contract. {@code DYNAMIC} typing is required because
     * the advice parameter is typed {@code Object} while the instrumented
     * constructor declares either {@code Map} or {@code Properties}.
     *
     * <p>Note on the constructor chain: {@code KafkaConsumer(Properties)} delegates
     * to another public constructor, so the enter advice runs twice — the outer
     * call copies the original into a fresh instance and the inner call copies
     * again. The double copy is harmless (idempotent {@code mergeInterceptors})
     * and invisible to the user.
     */
    @SuppressWarnings({"unused", "UnusedAssignment"})
    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(value = 0, readOnly = false, typing = Assigner.Typing.DYNAMIC) Object config) {
        config = InFlightLedger.prepareConsumerConfig(config);
    }

    /**
     * Registers the newly constructed consumer. Reads the config from
     * {@code args[0]}, which after the enter advice holds the augmented copy —
     * {@code bootstrap.servers} and {@code group.id} are preserved by the copy,
     * so identity extraction is unaffected. The {@code activate} guard in
     * {@link InFlightLedger#handleConsumerConstructorExit} keeps the inner link
     * of the delegation chain from being counted twice.
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodExit
    public static void onExit(@Advice.This Object consumer, @Advice.AllArguments Object[] args) {
        InFlightLedger.handleConsumerConstructorExit(consumer, args);
    }
}