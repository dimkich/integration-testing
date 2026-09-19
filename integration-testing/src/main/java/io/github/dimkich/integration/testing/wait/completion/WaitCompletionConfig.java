package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.wait.completion.future.like.FutureLikeWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.queue.like.QueueLikeWaitCompletion;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@Import({FutureLikeWaitCompletion.class, MethodCountingWaitCompletion.class, MethodPairWaitCompletion.class,
        QueueLikeWaitCompletion.class})
public class WaitCompletionConfig {
    @Configuration
    @ConditionalOnProperty(value = "integration.testing.wait.completion.enabled", havingValue = "false", matchIfMissing = true)
    static class Disabled {
        @Bean
        WaitCompletionList waitCompletion() {
            return new WaitCompletionList(List.of());
        }
    }

    @Configuration
    @ConditionalOnProperty(value = "integration.testing.wait.completion.enabled", havingValue = "true")
    static class Enabled implements BeanDefinitionRegistryPostProcessor {
        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(WaitCompletionList.class)
                    .setLazyInit(true);
            registry.registerBeanDefinition("waitCompletion", builder.getBeanDefinition());
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        }
    }
}
