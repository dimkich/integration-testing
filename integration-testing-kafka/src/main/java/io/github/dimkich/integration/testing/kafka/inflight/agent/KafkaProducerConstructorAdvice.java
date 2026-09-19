package io.github.dimkich.integration.testing.kafka.inflight.agent;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * Byte Buddy advice on {@code KafkaProducer} constructors: registers the in-flight
 * interceptor in the config argument and registers the constructed producer in the
 * ledger.
 */
public class KafkaProducerConstructorAdvice {

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
     */
    @SuppressWarnings({"unused", "UnusedAssignment"})
    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(value = 0, readOnly = false, typing = Assigner.Typing.DYNAMIC) Object config) {
        config = InFlightLedger.prepareProducerConfig(config);
    }

    /**
     * Registers the newly constructed producer. Reads the config from
     * {@code args[0]}, which after the enter advice holds the augmented copy —
     * {@code bootstrap.servers} and {@code transactional.id} are preserved by
     * the copy, so identity extraction is unaffected.
     */
    @SuppressWarnings("unused")
    @Advice.OnMethodExit
    public static void onExit(@Advice.This Object producer, @Advice.AllArguments Object[] args) {
        InFlightLedger.handleProducerConstructorExit(producer, args);
    }
}