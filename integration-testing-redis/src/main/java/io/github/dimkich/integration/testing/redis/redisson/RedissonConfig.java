package io.github.dimkich.integration.testing.redis.redisson;

import io.github.dimkich.integration.testing.ConditionalOnMockedServices;
import io.github.dimkich.integration.testing.ConditionalOnRealServices;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Config.class)
public class RedissonConfig implements BeanPostProcessor, BeanFactoryPostProcessor {
    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        for (String name : beanFactory.getBeanNamesForType(RedissonClient.class)) {
            AbstractBeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(RedissonDataStorage.class)
                    .addConstructorArgValue(name)
                    .getBeanDefinition();
            ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(name + "DataStorage", definition);
        }
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean instanceof RedissonClient) {
            ProxyFactory factory = new ProxyFactory(bean);
            factory.addAdvice(beanFactory.getBean(beanName + "DataStorage", RedissonDataStorage.class));
            return factory.getProxy();
        }
        return bean;
    }

    @Configuration
    @ConditionalOnRealServices
    public static class RealConfig {
    }

    @Configuration
    @ConditionalOnMockedServices
    public static class MockConfig implements BeanFactoryPostProcessor {
        private static MockedStatic<Redisson> redissonStatic;

        @Override
        public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory beanFactory) throws BeansException {
            if (redissonStatic != null) {
                redissonStatic.close();
            }
            redissonStatic = Mockito.mockStatic(Redisson.class);
            RedissonAnswer answer = new RedissonAnswer();
            redissonStatic.when(Redisson::create).thenAnswer(answer);
            redissonStatic.when(() -> Redisson.create(Mockito.any())).thenAnswer(answer);
        }

        @PreDestroy
        public void destroy() {
            redissonStatic.close();
            redissonStatic = null;
        }

        static class RedissonAnswer implements Answer<RedissonClient> {
            @Override
            public RedissonClient answer(InvocationOnMock invocation) {
                return Mockito.mock(RedissonClient.class, Mockito.withSettings()
                        .defaultAnswer(new RedissonClientAnswer()));
            }
        }
    }
}
