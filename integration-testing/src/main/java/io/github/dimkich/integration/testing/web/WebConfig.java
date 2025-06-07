package io.github.dimkich.integration.testing.web;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.Module;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.SpringHandlerInstantiator;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.WebApplicationContext;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass({ResponseEntity.class, SpringHandlerInstantiator.class, RestClientResponseException.class})
public class WebConfig {
    private final ConfigurableListableBeanFactory beanFactory;

    @Bean
    Module webTestModule() {
        SimpleModule jacksonModule = new SimpleModule();
        jacksonModule.setMixInAnnotation(ResponseEntity.class, ResponseEntityMixIn.class);
        jacksonModule.setMixInAnnotation(HttpStatusCode.class, ResponseEntityMixIn.HttpStatusMixIn.class);
        jacksonModule.setMixInAnnotation(RestClientResponseException.class, ResponseEntityMixIn.RestClientResponseExceptionMixin.class);
        jacksonModule.setMixInAnnotation(HttpMethod.class, HttpMethodMixIn.class);
        return new Module()
                .setHandlerInstantiator(new SpringHandlerInstantiator(beanFactory))
                .addJacksonModule(jacksonModule)
                .addSubTypes(ResponseEntity.class, HttpMethod.class);
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
}
