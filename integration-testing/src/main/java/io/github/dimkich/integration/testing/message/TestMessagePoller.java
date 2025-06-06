package io.github.dimkich.integration.testing.message;

import io.github.dimkich.integration.testing.Environment;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnMissingBean(TestMessagePoller.class)
public class TestMessagePoller {
    @Value("${integration.testing.environment}")
    private String environment;

    private final BlockingQueue<MessageDto<?>> messages = new ArrayBlockingQueue<>(1000);

    @SneakyThrows
    public MessageDto<?> pollMessage() {
        if (Environment.MOCK.equals(environment)) {
            return messages.poll(1, TimeUnit.NANOSECONDS);
        }
        return messages.poll(500, TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    public void putMessage(MessageDto<?> message) {
        if (!message.isTestInboundMessage()) {
            messages.put(message);
        }
    }

    public List<MessageDto<?>> pollMessages(int expectedCount) {
        List<MessageDto<?>> list = new ArrayList<>();
        messages.drainTo(list);
        for (int i = list.size(); i < expectedCount; i++) {
            MessageDto<?> message = pollMessage();
            if (message == null) {
                break;
            }
            list.add(message);
        }
        messages.drainTo(list);
        return list;
    }
}
