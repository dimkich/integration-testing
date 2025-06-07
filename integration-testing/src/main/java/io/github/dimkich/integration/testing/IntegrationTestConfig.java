package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.assertion.AssertionConfig;
import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.date.time.TestClockService;
import io.github.dimkich.integration.testing.execution.MockInvokeConfig;
import io.github.dimkich.integration.testing.initialization.InitializationConfig;
import io.github.dimkich.integration.testing.openapi.OpenApiConfig;
import io.github.dimkich.integration.testing.storage.StorageConfig;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletionConfig;
import io.github.dimkich.integration.testing.xml.XmlConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "integration.testing.enabled", havingValue = "true")
@Import({DynamicTestBuilder.class, XmlConfig.class, WaitCompletionConfig.class, StorageConfig.class, DateTimeService.class,
        InitializationConfig.class, MockInvokeConfig.class, OpenApiConfig.class, AssertionConfig.class, TestClockService.class})
public class IntegrationTestConfig {

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
