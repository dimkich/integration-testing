package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.execution.StubConfig;
import io.github.dimkich.integration.testing.storage.diff.DiffConfig;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import io.github.dimkich.integration.testing.storage.pojo.PojoAccessorService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TestDataStorages.class, DiffConfig.class, StubConfig.class,
        PojoAccessorService.class, FieldExclusionProcessor.class})
public class StorageConfig {
}
