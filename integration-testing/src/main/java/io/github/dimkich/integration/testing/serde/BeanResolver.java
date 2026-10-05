package io.github.dimkich.integration.testing.serde;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.StringUtils;

/**
 * Resolves beans by an optional name and a required type.
 *
 * <p>When the reference is blank, the single bean of the requested type is used; the resolution
 * fails with a descriptive message when there is no such bean or when several candidates exist.
 */
@RequiredArgsConstructor
public class BeanResolver {

    private final AutowireCapableBeanFactory beanFactory;

    /**
     * Resolves the bean referenced by name, or the unique bean of the given type.
     *
     * @param ref  the bean name, or {@code null}/blank to resolve by type
     * @param type the required bean type
     * @param <T>  the bean type
     * @return the resolved bean
     * @throws IllegalArgumentException if no bean or multiple beans are found
     */
    public <T> T resolve(String ref, Class<T> type) {
        if (StringUtils.hasText(ref)) {
            try {
                return beanFactory.getBean(ref, type);
            } catch (NoSuchBeanDefinitionException e) {
                throw new IllegalArgumentException(String.format(
                        "No bean named [%s] of type [%s] found. Check 'objectMapperRef' in configuration.",
                        ref, type.getName()), e);
            } catch (BeanNotOfRequiredTypeException e) {
                throw new IllegalArgumentException(String.format(
                        "Bean named [%s] is not of type [%s] but was actually of type [%s]. "
                                + "Check 'objectMapperRef' in configuration.",
                        e.getBeanName(), e.getRequiredType().getName(), e.getActualType().getName()), e);
            }
        }
        try {
            return beanFactory.getBean(type);
        } catch (NoUniqueBeanDefinitionException e) {
            throw new IllegalArgumentException(
                    String.format("Multiple beans of type [%s] found. Specify 'objectMapperRef' in configuration.",
                            type.getName()),
                    e);
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalArgumentException(
                    String.format("No bean of type [%s] found. Define a bean of this type or specify "
                            + "'objectMapperRef' in configuration.", type.getName()),
                    e);
        }
    }
}
