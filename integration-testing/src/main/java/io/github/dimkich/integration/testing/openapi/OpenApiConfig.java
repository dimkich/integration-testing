package io.github.dimkich.integration.testing.openapi;

import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Constructor;

@Configuration
public class OpenApiConfig implements BeanDefinitionRegistryPostProcessor {
    private BeanDefinitionRegistry registry;
    private final String apiFactoryBeanName = "apiFactory";
    private final String mockMvcClientHttpRequestFactoryBeanName = "mockMvcClientHttpRequestFactory";
    private String apiBeanName;
    private String restTemplateBeanName;
    private String apiClientBeanName;

    @Override
    @SneakyThrows
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
        for (TestOpenAPI testOpenAPI : JunitExtension.getTestOpenAPIS()) {
            Class<?> apiClass = testOpenAPI.apiClass();
            apiBeanName = apiClass.getSimpleName().substring(0, 1).toLowerCase() + apiClass.getSimpleName().substring(1);
            if (StringUtils.hasText(testOpenAPI.apiBeanName())) {
                apiBeanName = testOpenAPI.apiBeanName();
            }
            restTemplateBeanName = apiBeanName + "_RestTemplate";
            apiClientBeanName = apiBeanName + "_ApiClient";

            registerApi(testOpenAPI);
            registerRestTemplate(testOpenAPI);
            registerApiClient(testOpenAPI);
        }

        if (!JunitExtension.getTestOpenAPIS().isEmpty()) {
            registry.registerBeanDefinition(apiFactoryBeanName, new RootBeanDefinition(ApiFactory.class));

            RootBeanDefinition definition = new RootBeanDefinition(MockMvcClientHttpRequestFactory.class);
            definition.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference("mockMvc"));
            registry.registerBeanDefinition(mockMvcClientHttpRequestFactoryBeanName, definition);
        }
    }

    private void registerApi(TestOpenAPI testOpenAPI) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(testOpenAPI.apiClass())
                .setLazyInit(true)
                .addConstructorArgReference(apiClientBeanName);
        registry.registerBeanDefinition(apiBeanName + "_NoProxy", builder.getBeanDefinition());

        builder = BeanDefinitionBuilder.genericBeanDefinition(testOpenAPI.apiClass())
                .setLazyInit(true)
                .setFactoryMethodOnBean("createOpenApi", apiFactoryBeanName)
                .addConstructorArgReference(apiBeanName + "_NoProxy")
                .addConstructorArgReference(restTemplateBeanName)
                .addConstructorArgValue(testOpenAPI.errorResponseClass());

        registry.registerBeanDefinition(apiBeanName, builder.getBeanDefinition());
    }

    private void registerRestTemplate(TestOpenAPI testOpenAPI) {
        BeanDefinition restTemplateBeanDefinition;
        if (StringUtils.hasText(testOpenAPI.restTemplateBeanName())) {
            restTemplateBeanName = testOpenAPI.restTemplateBeanName();
            restTemplateBeanDefinition = registry.getBeanDefinition(restTemplateBeanName);
        } else {
            restTemplateBeanDefinition = new RootBeanDefinition(RestTemplate.class);
            restTemplateBeanDefinition.setLazyInit(true);
            registry.registerBeanDefinition(restTemplateBeanName, restTemplateBeanDefinition);
        }
        restTemplateBeanDefinition.getPropertyValues().addPropertyValue("requestFactory",
                new RuntimeBeanReference(mockMvcClientHttpRequestFactoryBeanName));
    }

    private void registerApiClient(TestOpenAPI testOpenAPI) {
        Class<?> apiClientClass = null;
        for (Constructor<?> constructor : testOpenAPI.apiClass().getConstructors()) {
            if (constructor.getParameterCount() == 1) {
                apiClientClass = constructor.getParameterTypes()[0];
            }
        }
        if (apiClientClass == null) {
            throw new RuntimeException("Unable to find constructor with ApiClient param in class " + testOpenAPI.apiClass().getName());
        }
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(apiClientClass)
                .setLazyInit(true)
                .addConstructorArgReference(restTemplateBeanName);
        if (StringUtils.hasText(testOpenAPI.basePath())) {
            builder.addPropertyValue("basePath", testOpenAPI.basePath());
        }
        registry.registerBeanDefinition(apiClientBeanName, builder.getBeanDefinition());
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
