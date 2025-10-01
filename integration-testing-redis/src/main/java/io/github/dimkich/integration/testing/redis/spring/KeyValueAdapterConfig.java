package io.github.dimkich.integration.testing.redis.spring;

import io.github.dimkich.integration.testing.redis.spring.ttl.KeyValueAdapterInterceptor;
import io.github.dimkich.integration.testing.redis.spring.ttl.NowSetterRedisNotifier;
import io.github.dimkich.integration.testing.redis.spring.ttl.TtlEntityService;
import jakarta.annotation.Nonnull;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueOperations;

@Configuration
@ConditionalOnClass(KeyValueOperations.class)
@Import(NowSetterRedisNotifier.class)
public class KeyValueAdapterConfig implements BeanPostProcessor, BeanFactoryPostProcessor {
    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean instanceof KeyValueAdapter keyValueAdapter) {
            ProxyFactory factory = new ProxyFactory();
            factory.setTarget(bean);
            TtlEntityService service = new TtlEntityService(keyValueAdapter);
            beanFactory.getBean(NowSetterRedisNotifier.class).addService(service);
            factory.addAdvice(new KeyValueAdapterInterceptor(service));
            return factory.getProxy();
        }
        return bean;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
