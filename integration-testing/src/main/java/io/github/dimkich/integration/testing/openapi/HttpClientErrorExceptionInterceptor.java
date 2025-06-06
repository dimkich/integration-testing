package io.github.dimkich.integration.testing.openapi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RestClientResponseException;

@RequiredArgsConstructor
public class HttpClientErrorExceptionInterceptor implements MethodInterceptor {
    private final HttpMessageConverterExtractor<?> extractor;

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (RestClientResponseException e) {
            if (invocation.getMethod().getReturnType() != ResponseEntity.class) {
                throw e;
            }
            MockClientHttpResponse response = new MockClientHttpResponse(e.getResponseBodyAsByteArray(), e.getStatusCode());
            if (e.getResponseHeaders() != null) {
                response.getHeaders().addAll(e.getResponseHeaders());
            }
            return new ResponseEntity<>(extractor.extractData(response), e.getResponseHeaders(), e.getStatusCode());
        }
    }
}