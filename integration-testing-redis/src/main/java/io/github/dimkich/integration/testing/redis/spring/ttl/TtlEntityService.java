package io.github.dimkich.integration.testing.redis.spring.ttl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.convert.RedisConverter;
import org.testcontainers.shaded.com.google.common.base.Optional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class TtlEntityService {
    private final KeyValueAdapter keyValueAdapter;

    private final Map<Class<?>, Optional<TtlEntityData>> entityData = new ConcurrentHashMap<>();
    private final Map<String, Map<Object, ZonedDateTime>> timeToLiveExpire = new ConcurrentHashMap<>();

    private ZonedDateTime dateTime = ZonedDateTime.now();

    public void saveTtl(Object id, Object entity, String keyspace) {
        TtlEntityData data = getTtl(entity);
        if (data == null) {
            return;
        }
        Number timeToLive = data.getTtl(entity);
        if (timeToLive != null) {
            Map<Object, ZonedDateTime> idToExpire = timeToLiveExpire.computeIfAbsent(keyspace, k -> new ConcurrentHashMap<>());
            idToExpire.put(id, dateTime.plus(Duration.of(timeToLive.longValue(), data.getChronoUnit())));
            data.setTtl(entity, null);
        }
    }

    public void restoreTtl(Object entity, String keyspace) {
        TtlEntityData data = getTtl(entity);
        if (data != null) {
            restoreTtl(data.getId(entity), entity, keyspace);
        }
    }

    public void restoreTtl(Object id, Object entity, String keyspace) {
        TtlEntityData data = getTtl(entity);
        if (data == null) {
            return;
        }
        ZonedDateTime expire = timeToLiveExpire.computeIfAbsent(keyspace, k -> new ConcurrentHashMap<>()).get(id);
        if (expire == null) {
            data.setTtl(entity, -1L);
            return;
        }
        data.setTtl(entity, data.getChronoUnit().between(dateTime, expire));
    }

    public void clear(String keyspace) {
        Map<Object, ZonedDateTime> map = timeToLiveExpire.get(keyspace);
        if (map != null) {
            map.clear();
        }
    }

    public RedisConverter getConverter() {
        return ((RedisKeyValueAdapter) keyValueAdapter).getConverter();
    }

    public void setNow(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
        for (Map.Entry<String, Map<Object, ZonedDateTime>> keyspaceEntry : timeToLiveExpire.entrySet()) {
            String keyspace = keyspaceEntry.getKey();
            for (Map.Entry<Object, ZonedDateTime> entry : keyspaceEntry.getValue().entrySet()) {
                Object id = entry.getKey();
                if (this.dateTime.isAfter(entry.getValue())) {
                    keyValueAdapter.delete(id, keyspace);
                }
            }
        }
    }

    private TtlEntityData getTtl(Object entity) {
        if (entity == null) {
            return null;
        }
        Optional<TtlEntityData> data = entityData.get(entity.getClass());
        if (data == null) {
            TtlEntityData ttlEntityData = TtlEntityData.tryCreateFromClass(entity.getClass());
            data = ttlEntityData == null ? Optional.absent() : Optional.of(ttlEntityData);
            entityData.put(entity.getClass(), data);
        }
        return data.orNull();
    }
}
