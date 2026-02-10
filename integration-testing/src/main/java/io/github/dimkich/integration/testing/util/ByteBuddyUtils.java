package io.github.dimkich.integration.testing.util;

import lombok.Getter;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.pool.TypePool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility methods for working with ByteBuddy in integration tests.
 * <p>
 * Provides helpers for opening Java modules, making members accessible for reflection,
 * moving classes to the bootstrap class loader and applying additional ASM visitors
 * to workaround JDK/ByteBuddy related issues.
 * </p>
 */
public class ByteBuddyUtils {
    /**
     * ASM visitor wrapper that fixes a bug in the JDK which forgets reconstruction of parameter names in some builds.
     * <p>
     * This wrapper attempts to use Mockito's {@code ParameterWritingVisitorWrapper} if available,
     * otherwise falls back to a no-op visitor. The wrapper is applied during bytecode transformation
     * to preserve parameter name information that would otherwise be lost.
     * </p>
     * <p>
     * For more details, see:
     * <a href="https://github.com/raphw/byte-buddy/issues/1562">ByteBuddy issue #1562</a>
     * </p>
     */
    @Getter
    public static final Function<TypeDescription, AsmVisitorWrapper> parameterWritingVisitorWrapper;

    static {
        Constructor<?> constructor;
        try {
            Class<?> cls = Class.forName("org.mockito.internal.creation.bytebuddy.InlineBytecodeGenerator$ParameterWritingVisitorWrapper");
            constructor = cls.getDeclaredConstructor(Class.class);
            constructor.setAccessible(true);
        } catch (Exception e) {
            constructor = null;
        }
        Constructor<?> c = constructor;
        parameterWritingVisitorWrapper = name -> {
            try {
                if (c != null && name instanceof TypeDescription.ForLoadedType) {
                    return (AsmVisitorWrapper.AbstractBase) c.newInstance(Class.forName(name.getName()));
                }
            } catch (ReflectiveOperationException ignore) {
            }
            return AsmVisitorWrapper.NoOp.INSTANCE;
        };
    }

    /**
     * Opens the module of the given class to the module of {@link ByteBuddyUtils},
     * if it is not already open.
     *
     * @param cls class whose module should be opened to this library
     */
    public static void openModuleOfClass(Class<?> cls) {
        if (!cls.getModule().isOpen(cls.getPackageName(), ByteBuddyUtils.class.getModule())) {
            ByteBuddyAgent.install().redefineModule(cls.getModule(), Set.of(), Map.of(),
                    Map.of(cls.getPackageName(), Set.of(ByteBuddyUtils.class.getModule())), Set.of(), Map.of());
        }
    }

    /**
     * Makes the supplied {@link Method} accessible and opens its declaring module if needed.
     *
     * @param method method to make accessible
     * @return the same method instance for fluent usage
     */
    public static Method makeAccessible(Method method) {
        openModuleOfClass(method.getDeclaringClass());
        method.setAccessible(true);
        return method;
    }

    /**
     * Makes the supplied {@link Constructor} accessible and opens its declaring module if needed.
     *
     * @param constructor constructor to make accessible
     * @param <T>         type being constructed
     * @return the same constructor instance for fluent usage
     */
    public static <T> Constructor<T> makeAccessible(Constructor<T> constructor) {
        openModuleOfClass(constructor.getDeclaringClass());
        constructor.setAccessible(true);
        return constructor;
    }

    /**
     * Makes the supplied {@link Field} accessible and opens its declaring module if needed.
     *
     * @param field field to make accessible
     * @return the same field instance for fluent usage
     */
    public static Field makeAccessible(Field field) {
        openModuleOfClass(field.getDeclaringClass());
        field.setAccessible(true);
        return field;
    }

    /**
     * Redefines the given classes and injects them into the bootstrap class loader.
     * <p>
     * Each class name is resolved using the system class loader and then transferred
     * to the bootstrap class loader via {@link ClassInjector.UsingUnsafe}.
     * </p>
     *
     * @param classNames fully qualified names of the classes to move
     */
    public static void moveClassToBootClassLoader(String... classNames) {
        TypePool typePool = TypePool.Default.ofSystemLoader();
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.ofSystemLoader();

        Map<TypeDescription, byte[]> allTypes = new HashMap<>();
        for (String className : classNames) {
            allTypes.putAll(new ByteBuddy()
                    .redefine(typePool.describe(className).resolve(), classFileLocator)
                    .make()
                    .getAllTypes());
        }

        if (!allTypes.isEmpty()) {
            ClassInjector.UsingUnsafe.ofBootLoader().inject(allTypes);
        }
    }
}