package io.github.dimkich.integration.testing.serde;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Resolves the generic arguments with which Spring beans implement a given interface.
 * Used by the serde subsystem to discover adapters and providers together with their
 * source, target and config types.
 */
@RequiredArgsConstructor
public class SerdeGenericResolver {
    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * Finds all beans implementing the given interface and resolves the interface's
     * generic arguments for each of them.
     *
     * @param interfaceType the interface to inspect
     * @return a stream of bean names with their resolved generic arguments
     * @throws IllegalArgumentException if the generic arguments cannot be resolved
     *                                  for a bean
     */
    public Stream<BeanGenerics> findAndResolveGenerics(Class<?> interfaceType) {
        return Arrays.stream(beanFactory.getBeanNamesForType(interfaceType))
                .map(name -> new BeanGenerics(name, resolveBeanGenerics(name, interfaceType)));
    }

    private Class<?>[] resolveBeanGenerics(String beanName, Class<?> interfaceType) {
        int expectedCount = interfaceType.getTypeParameters().length;
        Class<?>[] generics;

        if (beanFactory.containsBeanDefinition(beanName)) {
            ResolvableType beanType = beanFactory.getMergedBeanDefinition(beanName).getResolvableType();
            generics = beanType.as(interfaceType).resolveGenerics();
            if (allResolved(generics, expectedCount)) {
                return generics;
            }
        }

        Object bean = beanFactory.getBean(beanName);
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        generics = ResolvableType.forClass(targetClass).as(interfaceType).resolveGenerics();

        if (allResolved(generics, expectedCount)) {
            return generics;
        }

        throw new IllegalArgumentException(String.format(
                "Bean [%s] implements %s with %d type parameter(s), but resolved generics: %s",
                beanName, interfaceType.getSimpleName(), expectedCount, Arrays.toString(generics)));
    }

    private boolean allResolved(Class<?>[] generics, int expectedCount) {
        if (generics.length < expectedCount) {
            return false;
        }
        for (int i = 0; i < expectedCount; i++) {
            if (generics[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * A bean name together with the generic arguments with which the bean
     * implements the inspected interface.
     */
    @Value
    public static class BeanGenerics {
        String beanName;
        Class<?>[] generics;
    }
}