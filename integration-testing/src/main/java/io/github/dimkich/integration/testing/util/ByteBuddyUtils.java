package io.github.dimkich.integration.testing.util;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class ByteBuddyUtils {
    public static void openModuleOfClass(Class<?> cls) {
        if (!cls.getModule().isOpen(cls.getPackageName(), ByteBuddyUtils.class.getModule())) {
            ByteBuddyAgent.install().redefineModule(cls.getModule(), Set.of(), Map.of(),
                    Map.of(cls.getPackageName(), Set.of(ByteBuddyUtils.class.getModule())), Set.of(), Map.of());
        }
    }

    public static Method makeAccessible(Method method) {
        openModuleOfClass(method.getDeclaringClass());
        method.setAccessible(true);
        return method;
    }

    public static <T> Constructor<T> makeAccessible(Constructor<T> constructor) {
        openModuleOfClass(constructor.getDeclaringClass());
        constructor.setAccessible(true);
        return constructor;
    }
}
