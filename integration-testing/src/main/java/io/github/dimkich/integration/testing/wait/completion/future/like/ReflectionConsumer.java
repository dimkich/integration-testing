package io.github.dimkich.integration.testing.wait.completion.future.like;

import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import lombok.RequiredArgsConstructor;

/**
 * {@link SneakyConsumer} that invokes a no-arg method with the given name
 * on the supplied object using reflection.
 */
@RequiredArgsConstructor
public class ReflectionConsumer implements SneakyConsumer<Object, Exception> {

    /**
     * Name of the no-argument method to invoke on the consumed object.
     */
    private final String methodName;

    /**
     * Invokes the configured method on the given object instance.
     *
     * @param o object on which the method will be invoked
     * @throws ReflectiveOperationException if the method cannot be found or invoked
     */
    @Override
    public void accept(Object o) throws ReflectiveOperationException {
        o.getClass().getMethod(methodName).invoke(o);
    }
}
