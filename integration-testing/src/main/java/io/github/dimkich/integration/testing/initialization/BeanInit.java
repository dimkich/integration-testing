package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import lombok.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@ToString
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
        private final TestExecutor testExecutor;

        @Override
        public Class<BeanInit> getTestCaseInitClass() {
            return BeanInit.class;
        }

        @Override
        @SneakyThrows
        public void init(BeanInit testCaseInit) {
            testExecutor.setExecuting(true);
            try {
                for (BeanMethod beanMethod : testCaseInit.getBean()) {
                    Object bean = beanFactory.getBean(beanMethod.getName());
                    bean.getClass().getMethod(beanMethod.getMethod()).invoke(bean);
                }
            } finally {
                testExecutor.setExecuting(false);
            }
        }
    }
}
