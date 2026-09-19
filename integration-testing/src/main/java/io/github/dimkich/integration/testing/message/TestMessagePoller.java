package io.github.dimkich.integration.testing.message;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnMissingBean(TestMessagePoller.class)
public class TestMessagePoller {
    private final ArrayDeque<AbstractMessage> messages = new ArrayDeque<>();

    /**
     * Adds a message to the queue of accumulated messages.
     *
     * @param message the message to accumulate
     */
    public synchronized void putMessage(AbstractMessage message) {
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
}
