package io.github.dimkich.integration.testing.web;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import io.github.dimkich.integration.testing.web.jackson.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.json.SpringHandlerInstantiator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass({ResponseEntity.class, SpringHandlerInstantiator.class, RestClientResponseException.class,
        WebConfig.TestRestTemplateConfig.class})
public class WebConfig {
    private final ConfigurableListableBeanFactory beanFactory;

    @Bean
    TestSetupModule webTestModule() throws ClassNotFoundException {
        SimpleModule jacksonModule = new SimpleModule();
        jacksonModule.setMixInAnnotation(RequestEntity.class, RequestEntityMixIn.class);
        jacksonModule.setMixInAnnotation(ResponseEntity.class, ResponseEntityMixIn.class);
        jacksonModule.setMixInAnnotation(HttpStatusCode.class, HttpStatusMixIn.class);
        jacksonModule.setMixInAnnotation(RestClientResponseException.class, RestClientResponseExceptionMixin.class);
        jacksonModule.setMixInAnnotation(HttpClientErrorException.class, HttpClientErrorExceptionMixIn.class);
        jacksonModule.setMixInAnnotation(HttpMethod.class, HttpMethodMixIn.class);
        jacksonModule.setMixInAnnotation(HttpEntity.class, HttpEntityMixIn.class);
        jacksonModule.addDeserializer(LinkedMultiValueMapStringString.class, new MultiValueMapDeserializer());
        return new TestSetupModule()
                .setHandlerInstantiator(new SpringHandlerInstantiator(beanFactory))
                .addJacksonModule(jacksonModule)
                .addAlias(Class.forName("org.springframework.http.converter.ResourceHttpMessageConverter$1"), "resource")
                .addAlias(Class.forName("org.springframework.http.converter.ResourceHttpMessageConverter$2"), "resource")
                .addSubTypes(HttpClientErrorException.BadRequest.class, "httpClientErrorException.BadRequest")
                .addSubTypes(HttpClientErrorException.Unauthorized.class, "httpClientErrorException.Unauthorized")
                .addSubTypes(HttpClientErrorException.Forbidden.class, "httpClientErrorException.Forbidden")
                .addSubTypes(HttpClientErrorException.NotFound.class, "httpClientErrorException.NotFound")
                .addSubTypes(HttpClientErrorException.MethodNotAllowed.class, "httpClientErrorException.MethodNotAllowed")
                .addSubTypes(HttpClientErrorException.NotAcceptable.class, "httpClientErrorException.NotAcceptable")
                .addSubTypes(HttpClientErrorException.Conflict.class, "httpClientErrorException.Conflict")
                .addSubTypes(HttpClientErrorException.Gone.class, "httpClientErrorException.Gone")
                .addSubTypes(HttpClientErrorException.UnsupportedMediaType.class, "httpClientErrorException.UnsupportedMediaType")
                .addSubTypes(HttpClientErrorException.UnprocessableEntity.class, "httpClientErrorException.UnprocessableEntity")
                .addSubTypes(HttpClientErrorException.TooManyRequests.class, "httpClientErrorException.TooManyRequests")
                .addSubTypes(RequestEntity.class, ResponseEntity.class, HttpMethod.class, HttpEntity.class,
                        LinkedMultiValueMapStringString.class, LinkedMultiValueMapStringObject.class);
    }

    @Bean
    MockMvcBuilderCustomizer portCustomizer() {
        return builder -> builder.apply(new MockMvcConfigurer() {
            @Override
            public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context) {
                return request -> {
                    request.setLocalPort(request.getServerPort());
                    return request;
                };
            }
        });
    }

    @TestConfiguration
    public static class TestRestTemplateConfig implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            for (TestRestTemplate testRestTemplate : JunitExtension.getTestRestTemplates()) {
                String factoryBean = beanFactory.getBeanNamesForType(getClass())[0];
                AbstractBeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(RestTemplate.class)
                        .setFactoryMethodOnBean("createRestTemplate", factoryBean)
                        .addConstructorArgValue(testRestTemplate.basePath())
                        .addConstructorArgReference("mockMvc")
                        .getBeanDefinition();
                ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(testRestTemplate.beanName(), definition);
            }
        }

        RestTemplate createRestTemplate(String basePath, MockMvc mockMvc) {
            RestTemplate restTemplate = new RestTemplate();
            ClientHttpRequestFactory factory;
            if (JunitExtension.getSpringBootTest().webEnvironment().isEmbedded()) {
                factory = restTemplate.getRequestFactory();
            } else {
                factory = new MockMvcClientHttpRequestMultipartFactory(mockMvc);
            }
            restTemplate.setRequestFactory((u, m) -> {
                u = URI.create(basePath).resolve(u);
                return factory.createRequest(u, m);
            });
            return restTemplate;
        }
    }
}
