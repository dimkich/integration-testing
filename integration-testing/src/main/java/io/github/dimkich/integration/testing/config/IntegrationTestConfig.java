package io.github.dimkich.integration.testing.config;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.assertion.AssertionConfig;
import io.github.dimkich.integration.testing.date.time.DateTimeConfig;
import io.github.dimkich.integration.testing.execution.MockInvokeConfig;
import io.github.dimkich.integration.testing.format.TestFormatConfig;
import io.github.dimkich.integration.testing.initialization.InitializationConfig;
import io.github.dimkich.integration.testing.message.AbstractMessage;
import io.github.dimkich.integration.testing.openapi.OpenApiConfig;
import io.github.dimkich.integration.testing.serde.SerdeConfig;
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

/**
 * Main Spring configuration of the integration-testing framework. Active by default
 * and can be disabled with {@code integration.testing.enabled=false}. Imports all
 * subsystem configurations and declares shared beans such as the cloner.
 */
@Configuration
@ConditionalOnProperty(value = "integration.testing.enabled", havingValue = "true", matchIfMissing = true)
@Import({DynamicTestBuilder.class, WaitCompletionConfig.class, StorageConfig.class, DateTimeConfig.class,
        InitializationConfig.class, MockInvokeConfig.class, OpenApiConfig.class, AssertionConfig.class,
        WebConfig.class, TestFormatConfig.class, PropertyInheritanceMerger.class, SerdeConfig.class,
        PluginSpringIntegrator.class})
public class IntegrationTestConfig {
    @Bean
    TestSetupModule integrationTestModule() {
        return new TestSetupModule().addParentType(AbstractMessage.class);
    }

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
