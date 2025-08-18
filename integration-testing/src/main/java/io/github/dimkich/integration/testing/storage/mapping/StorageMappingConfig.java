package io.github.dimkich.integration.testing.storage.mapping;

import io.github.dimkich.integration.testing.TestSetupModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageMappingConfig {
    @Bean
    TestSetupModule storageMappingTestSetupModule() {
        return new TestSetupModule()
                .addSubTypes(EntriesObjectKeyObjectValue.class, EntriesStringKeyObjectValue.class,
                        MapStringKeyStringValue.class, MapStringKeyObjectValue.class);
    }
}
