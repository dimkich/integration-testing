package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.message.MessageDto;
import io.github.dimkich.integration.testing.message.TestMessagePoller;
import io.github.dimkich.integration.testing.message.TestMessageSender;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import io.github.sugarcubes.cloner.Cloner;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor
public class MockKafkaWaitCompletion implements WaitCompletion {
    @Qualifier("kafkaTestMessageSender")
    private final TestMessageSender sender;
    private final TestMessagePoller poller;
    private final Cloner cloner;

    private final BlockingQueue<MessageDto<?>> messages = new ArrayBlockingQueue<>(100);
    @Value("${integration.testing.kafka.mock.use.poller:true}")
    private boolean usePoller;

    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    public void addMessage(MessageDto<Object> message) {
        if (message.getPayload() != null) {
            message.setPayload(cloner.clone(message.getPayload()));
        }
        if (message.getHeaders().getKey() != null) {
            message.getHeaders().setKey(cloner.clone(message.getHeaders().getKey()));
        }
        if (usePoller) {
            poller.putMessage(message);
        }
        messages.add(message);
    }

    @Override
    public void start() {
        messages.clear();
    }

    @Override
    public boolean isAnyTaskStarted() {
        return !messages.isEmpty();
    }

    @Override
    @SneakyThrows
    public void waitCompletion() {
        if (!messages.isEmpty()) {
            Future<?> future = executor.submit(() -> {
                MessageDto<?> message;
                while ((message = this.messages.poll()) != null) {
                    if (sender.canSend(message)) {
                        sender.sendInboundMessage(message);
                    }
                }
            });
            future.get();
        }
    }
}
