package io.github.dimkich.integration.testing.redis;

import io.github.dimkich.integration.testing.ConditionalOnMockedServices;
import io.github.dimkich.integration.testing.ConditionalOnRealServices;
import io.github.dimkich.integration.testing.storage.TestMapDataStorage;
import jakarta.annotation.PreDestroy;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnClass(Config.class)
public class RedissonConfig implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String name : beanFactory.getBeanNamesForType(RMapCache.class)) {
            AbstractBeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(TestMapDataStorage.class)
                    .addConstructorArgValue(name)
                    .addConstructorArgReference(name)
                    .getBeanDefinition();
            ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("#" + name, definition);
        }
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
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            if (redissonStatic != null) {
                redissonStatic.close();
            }
            redissonStatic = Mockito.mockStatic(Redisson.class);
            redissonStatic.when(() -> Redisson.create(Mockito.any()))
                    .thenAnswer(a -> {
                        Map<String, RedissonRMapCache<?, ?>> map = new HashMap<>();
                        return Mockito.mock(RedissonClient.class, Mockito.withSettings()
                                .defaultAnswer(ans -> {
                                    if (ans.getMethod().getName().equals("getMapCache")) {
                                        return map.computeIfAbsent((String) ans.getArguments()[0], n -> new RedissonRMapCache<>());
                                    }
                                    throw new RuntimeException("RedissonClient method " + ans.getMethod().getName() + " not supported");
                                }));
                    });
        }

        @PreDestroy
        public void destroy() {
            redissonStatic.close();
            redissonStatic = null;
        }
    }
}
