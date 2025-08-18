package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.TestInit;
import io.github.dimkich.integration.testing.TestSetupModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({InitializationService.class, SqlStorageSetup.Init.class, SqlStorageInit.Init.class,
        DateTimeInit.Init.class, BeanInit.Init.class, KeyValueStorageInit.Init.class, MockInit.Init.class})
public class InitializationConfig {
    @Bean
    TestSetupModule initializationTestSetupModule() {
        return new TestSetupModule()
                .addParentType(TestInit.class)
                .addSubTypes(DateTimeInit.class, KeyValueStorageInit.class, BeanInit.class, MockInit.class,
                        SqlStorageSetup.class, SqlStorageInit.class);
    }
}
