package io.github.dimkich.integration.testing.redis.facade;

import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.impl.PingCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.RedisTestDataStorage;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSyncStateDelegator;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.mock;

@RequiredArgsConstructor
@Component("redisReplicationMockFacade")
public class RedisReplicationMockFacade {
    private final TestDataStorages testDataStorages;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final RedisSyncStateDelegator snapshotTestFactorySyncStateDelegator;
    private final Replicator replicatorMock = mock(Replicator.class);

    private boolean isStreamMode;

    @PostConstruct
    public void init() {
        snapshotTestFactorySyncStateDelegator.switchToSnapshot();
        isStreamMode = false;
        testDataStorages.addAffectedStorage(testDataStorages.getTestDataStorage(
                "snapshotTestFactory", RedisTestDataStorage.class));
    }

    public void event(Event event) throws Exception {
        if (event.getClass() == PingCommand.class) {
            return;
        }
        try {
            if (event instanceof Command && !isStreamMode) {
                snapshotTestFactorySyncStateDelegator.switchToStream();
                isStreamMode = true;
            } else if (!(event instanceof Command) && isStreamMode) {
                snapshotTestFactorySyncStateDelegator.switchToSnapshot();
                isStreamMode = false;
            }

            snapshotTestFactorySyncStateDelegator.onEvent(replicatorMock, event);
            testDataStorages.getTestDataStorage("snapshotTestFactory", RedisTestDataStorage.class).getCurrentValue(null);
        } catch (UnsupportedOperationException e) {
            if (e.getMessage().contains(PingCommand.class.getName())) {
                return;
            }
            throw e;
        }
    }
}
