package io.github.dimkich.integration.testing.redis.redisson;

import io.github.dimkich.integration.testing.TestDataStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.redisson.api.RObject;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RedissonDataStorage implements TestDataStorage, MethodInterceptor {
    @Getter
    private final String name;
    private final Map<String, RObject> redissonObjects = new LinkedHashMap<>();

    @Override
    public Map<Object, Object> getCurrentValue() {
        return redissonObjects.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> this.convert(e.getValue()), (a, b) -> a, LinkedHashMap::new));
    }

    private Object convert(RObject object) {
        if (object instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (object instanceof Map<?, ?> map) {
            return new LinkedHashMap<>(map);
        }
        return object;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void clear() {
        for (RObject object : redissonObjects.values()) {
            if (object instanceof Collection<?> collection) {
                collection.clear();
            }
            if (object instanceof Map<?, ?> map) {
                map.clear();
            }
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object object = invocation.proceed();
        if (object instanceof RObject rObject) {
            redissonObjects.put(rObject.getName(), rObject);
        }
        return object;
    }
}
