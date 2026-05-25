package io.github.dimkich.integration.testing.redis.replication.handler.stream;

import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.impl.*;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class StreamIgnoringHandler implements RedisStreamHandler<Command> {
    private static final Set<Class<? extends Command>> ignored = Set.of(PingCommand.class, MultiCommand.class,
            ExecCommand.class, ReplConfGetAckCommand.class, FunctionLoadCommand.class, FunctionDeleteCommand.class,
            FunctionFlushCommand.class, FunctionRestoreCommand.class, BLMoveCommand.class, MSetExCommand.class,
            HSetExCommand.class, SPublishCommand.class,
            XGroupCreateCommand.class, XGroupDestroyCommand.class, XGroupCreateConsumerCommand.class,
            XGroupDelConsumerCommand.class, XGroupSetIdCommand.class, XAckCommand.class,
            XAckDelCommand.class, XClaimCommand.class, XSetIdCommand.class);

    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return ignored.contains(eventClass);
    }

    @Override
    public void handle(Command event, RedisInMemoryStore store) {
    }
}
