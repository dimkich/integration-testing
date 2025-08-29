package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Import({MockService.class, JunitExecutable.class, TestExecutor.class})
@EnableConfigurationProperties(MockInvokeProperties.class)
public class MockInvokeConfig implements BeanFactoryPostProcessor, SmartInstantiationAwareBeanPostProcessor, PriorityOrdered {
    private MockService mockService;
    private ConfigurableListableBeanFactory beanFactory;

    private boolean initialized = false;
    private final Map<Class<?>, TestBeanMock> beanMocksByClass = new HashMap<>();
    private final Map<String, TestBeanMock> beanMocksByName = new HashMap<>();

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
        init();
        TestBeanMock beanMock = beanMocksByName.get(beanName);
        if (beanMock == null) {
            beanMock = beanMocksByClass.get(bean.getClass());
        }
        if (beanMock != null) {
            if (mockService == null) {
                mockService = beanFactory.getBean(MockService.class);
            }
            return mockService.createBeanMock(beanMock, bean, beanName);
        }
        return bean;
    }

    @SneakyThrows
    private void init() {
        if (initialized) {
            return;
        }
        for (TestBeanMock beanMock : JunitExtension.getBeanMocks()) {
            if (!beanMock.name().isEmpty()) {
                beanMocksByName.put(beanMock.name(), beanMock);
            } else {
                Class<?> cls = beanMock.mockClass();
                if (beanMock.mockClass() == Null.class) {
                    cls = Class.forName(beanMock.mockClassName());
                }
                beanMocksByClass.put(cls, beanMock);
            }
        }
        initialized = true;
    }
}
