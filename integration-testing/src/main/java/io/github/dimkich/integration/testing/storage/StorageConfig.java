package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.execution.MockInvokeConfig;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import io.github.dimkich.integration.testing.storage.mapping.StorageMappingConfig;
import io.github.dimkich.integration.testing.storage.pojo.PojoAccessorService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TestDataStorages.class, ObjectsDifference.class, MockInvokeConfig.class, StorageMappingConfig.class,
        PojoAccessorService.class, FieldExclusionProcessor.class, JacksonConverter.class})
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
}
