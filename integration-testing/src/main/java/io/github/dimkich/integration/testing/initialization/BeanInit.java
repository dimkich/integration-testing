package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestInit;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import lombok.*;
import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Getter
@Setter
@ToString
public class BeanInit extends TestInit {
    private List<BeanMethod> bean;

    @Data
    public static class BeanMethod {
        @JacksonXmlProperty(isAttribute = true)
        private String name;
        @JacksonXmlProperty(isAttribute = true)
        private String method;
    }

    @RequiredArgsConstructor
    public static class Init implements Initializer<BeanInit> {
        private final BeanFactory beanFactory;
        private final TestExecutor testExecutor;
        private final Set<BeanMethod> beanMethods = new LinkedHashSet<>();

        @Override
        public Class<BeanInit> getTestInitClass() {
            return BeanInit.class;
        }

        @Override
        public Integer getOrder() {
            return 10000;
        }

        @Override
        public void init(Stream<BeanInit> inits) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            beanMethods.clear();
            inits.forEach(init -> beanMethods.addAll(init.getBean()));
            testExecutor.setExecuting(true);
            try {
                for (BeanMethod beanMethod : beanMethods) {
                    Object bean = beanFactory.getBean(beanMethod.getName());
                    bean.getClass().getMethod(beanMethod.getMethod()).invoke(bean);
                }
            } finally {
                testExecutor.setExecuting(false);
            }
        }
    }
}
