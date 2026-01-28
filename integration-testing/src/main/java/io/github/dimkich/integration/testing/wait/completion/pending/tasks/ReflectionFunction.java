package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * {@link SneakyFunction} implementation that invokes a no-arg method on the given object via reflection
 * and converts the result to an {@code int} value.
 * <p>
 * The target method is looked up by {@link Class#getMethod(String, Class[])} using the provided
 * {@code methodName}. The method is made accessible via {@link ByteBuddyUtils#makeAccessible(java.lang.reflect.AccessibleObject)}
 * and then invoked on the supplied instance.
 * </p>
 * <p>
 * Supported return types:
 * <ul>
 *     <li>{@link Number} - its {@link Number#intValue()} is returned;</li>
 *     <li>{@link Collection} - its {@link Collection#size()} is returned.</li>
 * </ul>
 * For any other return type an {@link IllegalArgumentException} is thrown.
 * </p>
 */
@RequiredArgsConstructor
public class ReflectionFunction implements SneakyFunction<Object, Integer, Exception> {
    /**
     * Name of the no-argument method to invoke on the supplied object.
     */
    private final String methodName;

    /**
     * Applies this function to the given object by invoking the configured method via reflection.
     *
     * @param o target object whose method should be invoked
     * @return {@code int} value derived from the invoked method's result
     * @throws Exception                if reflective operations fail or the underlying method throws an exception
     * @throws IllegalArgumentException if the method returns neither {@link Number} nor {@link Collection}
     */
    @Override
    public Integer apply(Object o) throws Exception {
        Method method = o.getClass().getMethod(methodName);
        ByteBuddyUtils.makeAccessible(method);
        Object result = method.invoke(o);
        if (result instanceof Number number) {
            return number.intValue();
        } else if (result instanceof Collection<?> collection) {
            return collection.size();
        }
        throw new IllegalArgumentException(
                String.format("The %s#%s does not return a Number or a Collection",
                        o.getClass().getName(), methodName)
        );
    }
}
