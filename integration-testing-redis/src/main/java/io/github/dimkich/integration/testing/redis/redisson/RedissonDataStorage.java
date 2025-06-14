package io.github.dimkich.integration.testing.redis.redisson;

import io.github.dimkich.integration.testing.redis.redisson.convert.RBridge;
import io.github.dimkich.integration.testing.redis.redisson.convert.RBridgeFactory;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.redisson.api.RObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RedissonDataStorage implements KeyValueDataStorage, MethodInterceptor {
    @Getter
    private final String name;
    private final Map<String, RBridge> redissonObjects = new LinkedHashMap<>();

    @Override
    public Map<String, Object> getKeysData() {
        return redissonObjects.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(), (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public void putKeysData(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            RBridge bridge = redissonObjects.get(entry.getKey());
            bridge.set(entry.getValue());
        }
    }

    @Override
    public void clearAll() {
        for (RBridge object : redissonObjects.values()) {
            object.clear();
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object object = invocation.proceed();
        if (object instanceof RObject rObject) {
            redissonObjects.put(rObject.getName(), RBridgeFactory.create(rObject));
        }
        return object;
    }
}
