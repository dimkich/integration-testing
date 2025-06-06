package io.github.dimkich.integration.testing.kafka.wait.completion;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MessageCounter {
    private final Map<String, Integer> topicListeners = new HashMap<>();
    private final Map<String, Integer> topicToCount = new HashMap<>();

    public void addListener(String topic) {
        topicListeners.compute(topic, (t, c) -> c == null ? 1 : c + 1);
    }

    public synchronized void messageSent(String topic) {
        int count = topicListeners.getOrDefault(topic, 0);
        topicToCount.compute(topic, (t, c) -> c == null ? count : c + count);
        log.trace("Message(count = {}) send to to topic {}", topicToCount.get(topic), topic);
    }

    public synchronized void messageReceived(String topic) {
        topicToCount.compute(topic, (t, c) -> c == null ? 0 : c - 1);
        log.trace("Message received from topic {} (left messages = {})", topic, topicToCount.get(topic));
        if (isAllMessagesReceived()) {
            notifyAll();
        }
    }

    @SneakyThrows
    public synchronized void waitAllMessageToReceive() {
        log.trace("Wait all message to receive (topic to messages to receive {})", topicToCount);
        if (!isAllMessagesReceived()) {
            wait(30_000);
        }
        log.trace("Messages received (topic to messages to receive {})", topicToCount);
    }

    public synchronized boolean isAnyTaskStarted() {
        return !topicToCount.isEmpty();
    }

    public synchronized void clear() {
        topicToCount.clear();
    }

    private boolean isAllMessagesReceived() {
        return topicToCount.values().stream().filter(i -> i > 0).findFirst().orElse(null) == null;
    }
}
