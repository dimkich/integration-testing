package io.github.dimkich.integration.testing.initialization;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({InitializationService.class, SqlStorageSetup.Init.class, SqlStorageInit.Init.class,
        DateTimeInit.Init.class, BeanInit.Init.class, KeyValueStorageInit.Init.class, MockInit.Init.class})
public class InitializationConfig {
}
