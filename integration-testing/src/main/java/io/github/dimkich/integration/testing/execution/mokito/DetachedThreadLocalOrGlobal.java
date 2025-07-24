package io.github.dimkich.integration.testing.execution.mokito;

import lombok.AccessLevel;
import lombok.Setter;
import org.mockito.internal.util.concurrent.DetachedThreadLocal;

public class DetachedThreadLocalOrGlobal<T> extends DetachedThreadLocal<T> {
    private static final Thread thread = new Thread("Mockito global thread");
    @Setter(AccessLevel.PACKAGE)
    private static boolean global;

    public DetachedThreadLocalOrGlobal(Cleaner cleaner) {
        super(cleaner);
    }

    public static <T> DetachedThreadLocalOrGlobal<T> create(Cleaner cleaner) {
        return new DetachedThreadLocalOrGlobal<>(cleaner);
    }

    @Override
    public T get() {
        if (global) {
            return get(thread);
        }
        return super.get();
    }

    @Override
    public T get(Thread t) {
        if (global) {
            return super.get(thread);
        }
        return super.get(t);
    }

    @Override
    public void set(T value) {
        if (global) {
            getBackingMap().put(thread, value);
        }
        super.set(value);
    }

    @Override
    public void clear() {
        if (global) {
            getBackingMap().remove(thread);
        }
        super.clear();
    }

    @Override
    public T pushTo(Thread t) {
        if (global) {
            return super.pushTo(thread);
        }
        return super.pushTo(t);
    }

    @Override
    public void define(Thread t, T value) {
        if (global) {
            super.define(thread, value);
        }
        super.define(t, value);
    }

    @Override
    protected T initialValue(Thread t) {
        if (global) {
            return super.initialValue(thread);
        }
        return super.initialValue(t);
    }
}
