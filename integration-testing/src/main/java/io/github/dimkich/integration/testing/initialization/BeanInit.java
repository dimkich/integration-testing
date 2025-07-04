package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class BeanInit extends TestCaseInit {
    private List<BeanMethod> bean;

    @Data
    public static class BeanMethod {
        @JacksonXmlProperty(isAttribute = true)
        private String name;
        @JacksonXmlProperty(isAttribute = true)
        private String method;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<BeanInit> {
        private final BeanFactory beanFactory;

        @Override
        public Class<BeanInit> getTestCaseInitClass() {
            return BeanInit.class;
        }

        @Override
        @SneakyThrows
        public void init(BeanInit testCaseInit) {
            for (BeanMethod beanMethod : testCaseInit.getBean()) {
                Object bean = beanFactory.getBean(beanMethod.getName());
                bean.getClass().getMethod(beanMethod.getMethod()).invoke(bean);
            }
        }
    }
}
