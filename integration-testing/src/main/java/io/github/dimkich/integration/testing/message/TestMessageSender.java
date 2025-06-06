package io.github.dimkich.integration.testing.message;

public interface TestMessageSender {
    boolean canSend(MessageDto<?> message);

    void sendInboundMessage(MessageDto<?> message);
}
