package io.github.dimkich.integration.testing.message;

/**
 * Strategy interface for sending test messages into the system under test.
 * <p>
 * Implementations decide whether they can handle a particular {@link MessageDto}
 * and perform the actual sending of the inbound message.
 */
public interface TestMessageSender {

    /**
     * Checks whether this sender can handle the given message.
     *
     * @param message the message to check
     * @return {@code true} if this sender is able to send the message, {@code false} otherwise
     */
    boolean canSend(MessageDto<?> message);

    /**
     * Sends the given message as an inbound message into the system under test.
     *
     * @param message the message to send
     */
    void sendInboundMessage(MessageDto<?> message);
}
