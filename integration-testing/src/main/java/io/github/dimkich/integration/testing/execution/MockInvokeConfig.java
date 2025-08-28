package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

@Configuration
@Import({MockService.class, JunitExecutable.class, TestExecutor.class})
@EnableConfigurationProperties(MockInvokeProperties.class)
public class MockInvokeConfig implements BeanFactoryPostProcessor, SmartInstantiationAwareBeanPostProcessor, PriorityOrdered {
    private MockService mockService;
    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (JunitExtension.isMock(bean.getClass(), beanName)) {
            if (mockService == null) {
                mockService = beanFactory.getBean(MockService.class);
            }
            return mockService.createMock(bean, beanName, JunitExtension.isSpy(bean.getClass(), beanName), false);
        }
        return bean;
    }
}
