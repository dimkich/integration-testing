package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.assertion.AssertionConfig;
import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.date.time.TestClockService;
import io.github.dimkich.integration.testing.execution.MockInvokeConfig;
import io.github.dimkich.integration.testing.initialization.InitializationConfig;
import io.github.dimkich.integration.testing.openapi.OpenApiConfig;
import io.github.dimkich.integration.testing.storage.StorageConfig;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletionConfig;
import io.github.dimkich.integration.testing.web.WebConfig;
import io.github.dimkich.integration.testing.xml.XmlConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(value = "integration.testing.enabled", havingValue = "true", matchIfMissing = true)
@Import({DynamicTestBuilder.class, XmlConfig.class, WaitCompletionConfig.class, StorageConfig.class, DateTimeService.class,
        InitializationConfig.class, MockInvokeConfig.class, OpenApiConfig.class, AssertionConfig.class, TestClockService.class,
        WebConfig.class})
public class IntegrationTestConfig {
}
