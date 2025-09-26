package io.github.dimkich.integration.testing.redis.redisson;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.mockito.invocation.InvocationOnMock;
import org.redisson.api.RMap;
import org.redisson.client.codec.Codec;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
public class RMockInvoke {
    private Object targetObject;
    private final String method;
    private final String name;
    private final Codec codec;
    private final InvocationOnMock invocation;
    private final Object config;

    public Object getMock() {
        return invocation.getMock();
    }

    public Stream<Object> getArgs() {
        return Stream.of(invocation.getArguments());
    }

    public ValueHolder valueHolder() {
        return (ValueHolder) targetObject;
    }

    public ReentrantLock lock() {
        return (ReentrantLock) targetObject;
    }

    @SuppressWarnings("unchecked")
    public <K, V> ConcurrentMap<K, V> concurrentMap() {
        return (ConcurrentMap<K, V>) targetObject;
    }

    @SuppressWarnings("unchecked")
    public <K, V> RMap<K, V> rMap() {
        return (RMap<K, V>) invocation.getMock();
    }

    public int getArgCount() {
        return invocation.getArguments().length;
    }

    public Object getArg1() {
        return invocation.getArguments()[0];
    }

    public Object getArg2() {
        return invocation.getArguments()[1];
    }

    public Object getArg3() {
        return invocation.getArguments()[2];
    }

    public Object proxyCall() {
        return proxyCall(invocation.getArguments().length);
    }

    public Object proxyCall0() {
        return proxyCall(0);
    }

    public Object proxyCall1() {
        return proxyCall(1);
    }

    public Object proxyCall2() {
        return proxyCall(2);
    }

    public Object proxyCall3() {
        return proxyCall(3);
    }

    @SneakyThrows
    public Object proxyCall(int argNum) {
        Class<?>[] classes = Arrays.stream(invocation.getRawArguments())
                .limit(argNum)
                .map(Object::getClass)
                .toArray(Class[]::new);
        Method method = MethodUtils.getMatchingAccessibleMethod(targetObject.getClass(), this.method, classes);
        if (method == null) {
            throw new NoSuchMethodException("Method " + this.method + Arrays.toString(classes) + " not found");
        }
        return method.invoke(targetObject, Arrays.copyOf(invocation.getRawArguments(), argNum));
    }
}
