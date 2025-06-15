package io.github.dimkich.integration.testing.initialization;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({InitializationService.class, DateTimeInit.Initializer.class, SqlStorageInit.Initializer.class,
        KeyValueStorageInit.Initializer.class, BeanInit.Initializer.class,
        SqlStorageSetup.Initializer.class})
public class InitializationConfig {
}
