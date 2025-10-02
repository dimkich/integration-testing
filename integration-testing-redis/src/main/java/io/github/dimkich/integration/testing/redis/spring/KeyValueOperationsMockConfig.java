package io.github.dimkich.integration.testing.redis.spring;

import io.github.dimkich.integration.testing.ConditionalOnMockedServices;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.keyvalue.repository.query.KeyValuePartTreeQuery;
import org.springframework.data.keyvalue.repository.query.SpelQueryCreator;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactoryBean;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.convert.MappingRedisConverter;
import org.springframework.data.redis.repository.support.RedisRepositoryFactoryBean;

@Configuration
@ConditionalOnMockedServices
@ConditionalOnClass(KeyValueOperations.class)
public class KeyValueOperationsMockConfig implements BeanFactoryPostProcessor {

    @Override
    @SneakyThrows
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        for (String name : beanFactory.getBeanNamesForType(MappingRedisConverter.class)) {
            registry.removeBeanDefinition(name);
        }

        for (String name : beanFactory.getBeanNamesForType(RedisKeyValueTemplate.class)) {
            BeanDefinition definition = registry.getBeanDefinition(name);
            definition.setBeanClassName(KeyValueTemplate.class.getName());

            ConstructorArgumentValues args = definition.getConstructorArgumentValues();
            for (int i = 0; i < definition.getConstructorArgumentValues().getArgumentCount(); i++) {
                ConstructorArgumentValues.ValueHolder valueHolder = args.getArgumentValue(i, Object.class);
                if (valueHolder != null && valueHolder.getValue() instanceof RuntimeBeanReference reference) {
                    BeanDefinition argDefinition = registry.getBeanDefinition(reference.getBeanName());
                    Class<?> argClass = Class.forName(argDefinition.getBeanClassName());
                    if (KeyValueAdapter.class.isAssignableFrom(argClass)) {
                        argDefinition.setBeanClassName(LinkedMapKeyValueAdapter.class.getName());
                        argDefinition.getConstructorArgumentValues().clear();
                        argDefinition.getPropertyValues().getPropertyValueList().clear();
                    } else if (MappingContext.class.isAssignableFrom(argClass)) {
                        argDefinition.setBeanClassName(KeyValueMappingContext.class.getName());
                        argDefinition.getConstructorArgumentValues().clear();
                        argDefinition.getPropertyValues().getPropertyValueList().clear();
                    } else {
                        throw new BeanCreationException("Can't mock Redis, unsupported argument type: "
                                + argClass.getName());
                    }
                } else {
                    throw new BeanCreationException("Can't mock Redis, unsupported argument type: " + valueHolder);
                }
            }
        }
        for (String name : beanFactory.getBeanNamesForType(RedisRepositoryFactoryBean.class)) {
            if (!registry.containsBeanDefinition(name)) {
                name = name.substring(1);
            }
            RootBeanDefinition definition = (RootBeanDefinition) registry.getBeanDefinition(name);
            registry.removeBeanDefinition(name);

            RootBeanDefinition newDefinition = new RootBeanDefinition(KeyValueRepositoryFactoryBean.class);
            newDefinition.setTargetType(definition.getResolvableType().getSuperType());
            newDefinition.setConstructorArgumentValues(definition.getConstructorArgumentValues());
            newDefinition.setPropertyValues(definition.getPropertyValues());
            Class<?> queryCreator = SpelQueryCreator.class;
            try {
                queryCreator = Class.forName("org.springframework.data.keyvalue.repository.query" +
                        ".PredicateQueryCreator");
            } catch (ClassNotFoundException ignore) {
            }
            newDefinition.getPropertyValues().addPropertyValue("queryCreator", queryCreator);
            newDefinition.getPropertyValues().addPropertyValue("queryType", KeyValuePartTreeQuery.class);
            registry.registerBeanDefinition(name, newDefinition);
        }
    }
}
