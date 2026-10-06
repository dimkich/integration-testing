package io.github.dimkich.integration.testing.redis.facade;

import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.impl.PingCommand;
import com.moilioncircle.redis.replicator.event.Event;
import io.github.dimkich.integration.testing.redis.RedisTestDataStorage;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.replication.RedisSyncBarrier;
import io.github.dimkich.integration.testing.redis.replication.RedisSyncState;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSyncStateDelegator;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.mock;

@RequiredArgsConstructor
@Component("redisReplicationMockFacade")
public class RedisReplicationMockFacade {
    private final TestDataStorages testDataStorages;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final RedisSyncStateDelegator snapshotTestFactorySyncStateDelegator;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final RedisSyncState snapshotTestFactorySyncState;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final RedisSyncBarrier snapshotTestFactorySyncBarrier;
    private final RedisProperties redisProperties;
    private final Replicator replicatorMock = mock(Replicator.class);

    private boolean isStreamMode;

    @PostConstruct
    public void init() {
        snapshotTestFactorySyncStateDelegator.switchToSnapshot();
        isStreamMode = false;
        testDataStorages.addAffectedStorage(testDataStorages.getTestDataStorage(
                "snapshotTestFactory", RedisTestDataStorage.class));
    }

    @SneakyThrows
    public void event(Event event) {
        if (event.getClass() == PingCommand.class) {
            return;
        }
        if (event instanceof Command && !isStreamMode) {
            snapshotTestFactorySyncStateDelegator.switchToStream();
            isStreamMode = true;
        } else if (!(event instanceof Command) && isStreamMode) {
            snapshotTestFactorySyncStateDelegator.switchToSnapshot();
            isStreamMode = false;
        }

        snapshotTestFactorySyncStateDelegator.onEvent(replicatorMock, event);
        snapshotTestFactorySyncState.throwFatalError();
        snapshotTestFactorySyncBarrier.triggerAndAwait(redisProperties.getSyncBarrierTimeoutMs());
    }
}
