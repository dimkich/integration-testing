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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
public class MockKafkaWaitCompletion implements WaitCompletion {
    @Qualifier("kafkaTestMessageSender")
    private final TestMessageSender sender;
    private final TestMessagePoller poller;
    private final Cloner cloner;

    private final List<MessageDto<?>> messages;
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
                List<MessageDto<?>> messages = new ArrayList<>(this.messages);
                this.messages.clear();
                for (MessageDto<?> message : messages) {
                    if (sender.canSend(message)) {
                        sender.sendInboundMessage(message);
                    }
                }
            });
            future.get();
        }
    }
}
