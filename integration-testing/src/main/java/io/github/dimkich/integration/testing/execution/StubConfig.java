package io.github.dimkich.integration.testing.execution;

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
@Import({StubService.class, TestExecutor.class})
@EnableConfigurationProperties(StubProperties.class)
public class StubConfig implements BeanFactoryPostProcessor, SmartInstantiationAwareBeanPostProcessor, PriorityOrdered {
    private StubService stubService;
    private ConfigurableListableBeanFactory beanFactory;

    private boolean initialized = false;
    private final Map<Class<?>, StubBean> beanStubsByClass = new HashMap<>();
    private final Map<String, StubBean> beanStubsByName = new HashMap<>();

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        init();
        StubBean beanStub = beanStubsByName.get(beanName);
        if (beanStub == null) {
            beanStub = beanStubsByClass.get(bean.getClass());
        }
        if (beanStub != null) {
            if (stubService == null) {
                stubService = beanFactory.getBean(StubService.class);
            }
            return stubService.createBeanStub(beanStub, bean, beanName);
        }
        return bean;
    }

    @SneakyThrows
    private void init() {
        if (initialized) {
            return;
        }
        for (StubBean beanStub : JunitExtension.getBeanStubs()) {
            if (!beanStub.name().isEmpty()) {
                beanStubsByName.put(beanStub.name(), beanStub);
            } else {
                Class<?> cls = beanStub.stubClass();
                if (beanStub.stubClass() == Null.class) {
                    cls = Class.forName(beanStub.stubClassName());
                }
                beanStubsByClass.put(cls, beanStub);
            }
        }
        initialized = true;
    }
}
