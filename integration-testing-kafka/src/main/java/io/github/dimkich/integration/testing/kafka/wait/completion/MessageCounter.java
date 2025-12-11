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
        log.trace("Message send to to topic {} (messages = {})", topic, topicToCount);
    }

    public synchronized void messageReceived(String topic) {
        Integer newValue = topicToCount.compute(topic, (t, c) -> c == null ? 0 : c - 1);
        if (newValue <= 0) {
            topicToCount.remove(topic);
        }
        log.trace("Message received from topic {} (messages = {})", topic, topicToCount);
        if (topicToCount.isEmpty()) {
            notifyAll();
        }
    }

    @SneakyThrows
    public synchronized void waitAllMessageToReceive() {
        log.trace("Wait all message to receive (messages = {})", topicToCount);
        if (!topicToCount.isEmpty()) {
            wait(30_000);
        }
        log.trace("Stop waiting all message to received (messages = {})", topicToCount);
    }

    public synchronized boolean isAnyTaskStarted() {
        return !topicToCount.isEmpty();
    }

    public synchronized void clear() {
        topicToCount.clear();
    }
}
