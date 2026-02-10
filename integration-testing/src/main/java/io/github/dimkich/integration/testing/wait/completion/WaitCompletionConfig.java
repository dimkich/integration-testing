package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.wait.completion.future.like.FutureLikeWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.queue.like.QueueLikeWaitCompletion;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

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
    @RequiredArgsConstructor
    @EnableConfigurationProperties(WaitCompletionProperties.class)
    @ConditionalOnProperty(value = "integration.testing.wait.completion.enabled", havingValue = "true")
    static class Enabled implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
        private Environment environment;
        private BeanDefinitionRegistry registry;

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            this.registry = registry;
            WaitCompletionProperties properties = Binder.get(environment)
                    .bind(WaitCompletionProperties.class.getAnnotation(ConfigurationProperties.class).prefix(),
                            Bindable.of(WaitCompletionProperties.class))
                    .orElseThrow(IllegalStateException::new);

            String env = environment.getProperty("integration.testing.environment");
            if (properties.isKafkaStandardTask() && io.github.dimkich.integration.testing.Environment.REAL.equals(env)) {
                registerAdvisorDefinition("execution(* org.springframework.kafka.core.KafkaTemplate.send(..))",
                        "kafkaTemplateInterceptor");
            }
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(WaitCompletionList.class)
                    .setLazyInit(true);
            registry.registerBeanDefinition("waitCompletion", builder.getBeanDefinition());
        }

        private void registerAdvisorDefinition(String expression, String advice) {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression(expression);
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DefaultPointcutAdvisor.class)
                    .setLazyInit(true)
                    .addConstructorArgValue(pointcut)
                    .addConstructorArgReference(advice);
            registry.registerBeanDefinition(advice + "_Advisor", builder.getBeanDefinition());
        }

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        }
    }
}
