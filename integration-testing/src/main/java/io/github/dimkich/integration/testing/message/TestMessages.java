package io.github.dimkich.integration.testing.message;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sends inbound messages into the system under test and accumulates messages captured by
 * the transports, filtering out messages produced by the test itself.
 * <p>
 * An inbound message is routed to the matching {@link TestMessageSender} by {@link #send(AbstractMessage)},
 * which also registers the transport identity of the message. A transport sniffer passes every
 * captured message to {@link #putMessage(AbstractMessage, boolean)}; a message whose
 * {@link AbstractMessage#identity()} was registered is treated as an echo of a message
 * produced by the test and dropped. Sending happens under the same monitor, so a sniffer
 * cannot observe the echo before its identity is registered.
 */
@Component
@RequiredArgsConstructor
public class TestMessages {
    private final ObjectProvider<TestMessageSender> testMessageSenders;
    private final ArrayDeque<AbstractMessage> messages = new ArrayDeque<>();
    private final Set<Object> inboundIdentities = new HashSet<>();

    /**
     * Adds a captured message to the queue unless it echoes a message produced by the test.
     * The identity is not consumed, so a message observed through several transports
     * at once is filtered everywhere it is captured.
     *
     * @param message       the captured message
     * @param filterInbound when {@code false}, the message is captured even if it echoes
     *                      a message produced by the test
     */
    public synchronized void putMessage(AbstractMessage message, boolean filterInbound) {
        Object identity = message.identity();
        if (filterInbound && identity != null && inboundIdentities.contains(identity)) {
            return;
        }
        messages.addLast(message);
    }

    /**
     * Returns all messages accumulated by the time of the call.
     * Synchronization has already been performed by WaitCompletion.
     *
     * @return the collected messages
     */
    public synchronized List<AbstractMessage> pollMessages() {
        List<AbstractMessage> list = new ArrayList<>(messages);
        messages.clear();
        return list;
    }

    /**
     * Sends the message into the system under test using the matching sender and registers
     * its transport identity under the same monitor. Because sending happens inside the
     * synchronized block, a sniffer cannot observe the message before its identity is
     * registered: either sending has not completed yet (the sniffer blocks on the monitor),
     * or it has, and the identity is already registered.
     * <p>
     * Senders are resolved lazily, on the first send, so that a sender which depends on
     * this bean (directly or transitively) does not create a circular dependency.
     *
     * @param message the message to send
     */
    public synchronized void send(AbstractMessage message) {
        TestMessageSender sender = testMessageSenders.orderedStream()
                .filter(s -> s.canSend(message))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No service found for message " + message));
        Object identity = sender.sendInboundMessage(message);
        if (identity != null) {
            inboundIdentities.add(identity);
        }
    }

    /**
     * Clears registered identities before a new test.
     */
    public synchronized void resetInbound() {
        inboundIdentities.clear();
    }
}
