package io.github.dimkich.integration.testing.redis.facade;

import com.moilioncircle.redis.replicator.Replicator;
import io.github.dimkich.integration.testing.redis.replication.RedisSyncBarrier;
import lombok.NonNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.*;

@Component
public class RedisReplicatorSpyProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        if (bean instanceof Replicator && "snapshotTestFactoryReplicator".equals(beanName)) {
            return mock(Replicator.class);
        }
        if (bean instanceof RedisSyncBarrier && "snapshotTestFactorySyncBarrier".equals(beanName)) {
            RedisSyncBarrier spyBarrier = spy((RedisSyncBarrier) bean);
            doNothing().when(spyBarrier).activate();
            return spyBarrier;
        }
        return bean;
    }
}
