package io.github.dimkich.integration.testing.storage.keyvalue;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.keyvalue.core.KeyValueOperations;

@Configuration(KeyValueOperationsConfig.beanName)
@Import(KeyValueOperationsConfig.Enabled.class)
public class KeyValueOperationsConfig {
    public static final String beanName = "keyValueOperationsConfig";

    @ConditionalOnClass(KeyValueOperations.class)
    static class Enabled implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            for (String name : beanFactory.getBeanNamesForType(KeyValueOperations.class)) {
                AbstractBeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(KeyValueOperationsDataStorage.class)
                        .addConstructorArgValue(name)
                        .addConstructorArgReference(name)
                        .getBeanDefinition();
                ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("#" + name, definition);
            }
        }
    }
}