package io.github.dimkich.integration.testing.redis.redisson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.redisson.api.RFuture;
import org.redisson.client.codec.Codec;
import org.redisson.misc.CompletableFutureWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public class RedissonMock {
    @Getter
    private final Class<?> targetClass;
    private final Map<String, Function<RMockInvoke, Object>> calls = new HashMap<>();

    public RedissonMock add(String method, Function<RMockInvoke, Object> call) {
        calls.put(method, call);
        return this;
    }

    public RedissonMock add(RedissonMock redissonMock) {
        calls.putAll(redissonMock.calls);
        return this;
    }

    public Set<String> getMethods() {
        return calls.keySet();
    }

    public Object call(Object targetObject, String name, Codec codec, InvocationOnMock invocation, Object config) {
        String method = invocation.getMethod().getName();
        if (method.endsWith("Async")) {
            method = method.substring(0, method.length() - 5);
        }
        if (!calls.containsKey(method)) {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }
        Object result = calls.get(method).apply(new RMockInvoke(targetObject, method, name, codec, invocation, config));
        if (RFuture.class.isAssignableFrom(invocation.getMethod().getReturnType())) {
            return new CompletableFutureWrapper<>(result);
        }
        return result;
    }
}
