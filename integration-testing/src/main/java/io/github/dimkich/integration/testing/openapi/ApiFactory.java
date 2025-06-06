package io.github.dimkich.integration.testing.openapi;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.validation.FieldError;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;

public class ApiFactory {
    public Object createOpenApi(Object openApi, RestTemplate restTemplate, Type type) {
        restTemplate.getMessageConverters().stream()
                .filter(c -> c instanceof AbstractJackson2HttpMessageConverter)
                .map(c -> (AbstractJackson2HttpMessageConverter) c)
                .forEach(c -> c.getObjectMapper().addMixIn(FieldError.class, FieldErrorMixIn.class));
        ProxyFactory factory = new ProxyFactory(openApi);
        HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor<>(type, restTemplate.getMessageConverters());
        factory.addAdvice(new HttpClientErrorExceptionInterceptor(extractor));
        return factory.getProxy();
    }
}
