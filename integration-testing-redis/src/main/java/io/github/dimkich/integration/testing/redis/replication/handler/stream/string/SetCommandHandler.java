package io.github.dimkich.integration.testing.redis.replication.handler.stream.string;

import com.moilioncircle.redis.replicator.cmd.impl.SetCommand;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.ExpiredType;
import io.github.dimkich.integration.testing.redis.replication.RedisInMemoryStore;
import io.github.dimkich.integration.testing.redis.replication.event.listener.RedisStreamHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class SetCommandHandler implements RedisStreamHandler<SetCommand> {
    @Override
    public boolean canHandle(Class<? extends Event> eventClass) {
        return SetCommand.class == eventClass;
    }

    @Override
    public void handle(SetCommand event, RedisInMemoryStore store) {
        store.compute(event.getKey(), (schema, entry) -> {
            entry.setValue(schema, event.getValue());

            ZonedDateTime now = store.getNow();

            if (event.getExpiredType() == ExpiredType.SECOND) {
                entry.setExpireAt(now, now.plusSeconds(event.getExpiredValue()));
            } else if (event.getExpiredType() == ExpiredType.MS) {
                entry.setExpireAt(now, now.plus(event.getExpiredValue(), ChronoUnit.MILLIS));
            } else if (event.getXatType() != null) {
                if (event.getXatType().name().equals("EXAT")) {
                    entry.setExpireAt(now, Instant.ofEpochSecond(event.getXatValue()).atZone(now.getZone()));
                } else if (event.getXatType().name().equals("PXAT")) {
                    entry.setExpireAt(now, Instant.ofEpochMilli(event.getXatValue()).atZone(now.getZone()));
                }
            } else if (!event.getKeepTtl()) {
                entry.setExpireAt(now, (ZonedDateTime) null);
            }
        });
    }
}