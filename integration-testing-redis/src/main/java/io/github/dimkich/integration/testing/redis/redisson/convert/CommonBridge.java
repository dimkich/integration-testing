package io.github.dimkich.integration.testing.redis.redisson.convert;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.redisson.api.RObject;

import java.lang.reflect.Method;

public class CommonBridge implements RBridge {
    private final RObject object;
    private final Method setMethod;
    private final Method getMethod;

    public CommonBridge(RObject object) {
        this.object = object;
        setMethod = MethodUtils.getMatchingAccessibleMethod(object.getClass(), "set", Object.class);
        getMethod = MethodUtils.getMatchingAccessibleMethod(object.getClass(), "get");
    }

    @Override
    @SneakyThrows
    public void set(Object value) {
        setMethod.invoke(object, value);
    }

    @Override
    @SneakyThrows
    public Object get() {
        return getMethod.invoke(object);
    }

    @Override
    public void clear() {
        set(null);
    }
}
