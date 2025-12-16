package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.assertion.AssertionConfig;
import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.execution.MockInvokeConfig;
import io.github.dimkich.integration.testing.format.TestFormatConfig;
import io.github.dimkich.integration.testing.initialization.InitializationConfig;
import io.github.dimkich.integration.testing.openapi.OpenApiConfig;
import io.github.dimkich.integration.testing.storage.StorageConfig;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletionConfig;
import io.github.dimkich.integration.testing.web.WebConfig;
import io.github.sugarcubes.cloner.Cloner;
import io.github.sugarcubes.cloner.Cloners;
import io.github.sugarcubes.cloner.ReflectionClonerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@ConditionalOnProperty(value = "integration.testing.enabled", havingValue = "true", matchIfMissing = true)
@Import({DynamicTestBuilder.class, WaitCompletionConfig.class, StorageConfig.class, DateTimeService.class,
        InitializationConfig.class, MockInvokeConfig.class, OpenApiConfig.class, AssertionConfig.class,
        WebConfig.class, TestFormatConfig.class})
public class IntegrationTestConfig {
    @Bean
    Cloner sugarCubesCloner(List<TestSetupModule> modules) {
        ReflectionClonerBuilder builder = Cloners.builder();
        for (TestSetupModule module : modules) {
            module.getFieldActions().forEach(builder::fieldAction);
            module.getTypeActions().forEach(builder::typeAction);
            module.getPredicateTypeActions().forEach(builder::typeAction);
        }
        return builder.build();
    }
}
