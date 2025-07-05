package io.github.dimkich.integration.testing.redis.redisson;

import io.github.dimkich.integration.testing.ConditionalOnMockedServices;
import io.github.dimkich.integration.testing.ConditionalOnRealServices;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.jcache.JCache;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
            ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("#" + name, definition);
        }
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean instanceof Redisson) {
            RedissonDataStorage storage = beanFactory.getBean("#" + beanName, RedissonDataStorage.class);
            if (MockUtil.isMock(bean)) {
                RedissonClientAnswer answer = (RedissonClientAnswer) MockUtil.getMockSettings(bean).getDefaultAnswer();
                answer.setStorage(storage);
            } else {
                return Mockito.mock(Redisson.class, Mockito.withSettings()
                        .defaultAnswer((Answer<Object>) i -> {
                            Object o = i.callRealMethod();
                            storage.tryPut(o);
                            return o;
                        }).spiedInstance(bean));
            }
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
        private static MockedConstruction<JCache> jCacheMock;

        @Override
        public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory beanFactory) throws BeansException {
            if (redissonStatic != null) {
                redissonStatic.close();
            }
            redissonStatic = Mockito.mockStatic(Redisson.class);
            RedissonAnswer answer = new RedissonAnswer();
            redissonStatic.when(Redisson::create).thenAnswer(answer);
            redissonStatic.when(() -> Redisson.create(Mockito.any())).thenAnswer(answer);

            if (jCacheMock != null) {
                jCacheMock.close();
            }
            jCacheMock = Mockito.mockConstruction(JCache.class,
                    Mockito.withSettings().defaultAnswer(new RedissonObjectAnswer()),
                    (mock, context) -> {
                        RedissonObjectAnswer ans = (RedissonObjectAnswer) MockUtil.getMockSettings(mock)
                                .getDefaultAnswer();
                        List<?> arg = context.arguments();
                        ans.setTargetObject(new ConcurrentHashMap<>());
                        Config config = ((Redisson) arg.get(1)).getConfig();
                        ans.setCodec(config == null ? null : config.getCodec());
                        ans.setName((String) arg.get(2));
                        ans.setRedissonMock(RedissonClientAnswer.redissonMockFactory.getJCache());
                        ans.setConfig(arg.get(3));
                    });
        }

        @PreDestroy
        public void destroy() {
            redissonStatic.close();
            redissonStatic = null;
            jCacheMock.close();
            jCacheMock = null;
        }

        static class RedissonAnswer implements Answer<RedissonClient> {
            @Override
            public RedissonClient answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments().length == 0) {
                    return (RedissonClient) invocation.callRealMethod();
                }
                Config config = (Config) invocation.getArguments()[0];
                return Mockito.mock(Redisson.class, Mockito.withSettings()
                        .defaultAnswer(new RedissonClientAnswer(config)));
            }
        }
    }
}
