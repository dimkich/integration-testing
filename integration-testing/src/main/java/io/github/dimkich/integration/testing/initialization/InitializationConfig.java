package io.github.dimkich.integration.testing.initialization;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({InitializationService.class, DateTimeInit.Initializer.class, TablesStorageInit.Initializer.class,
        MapStorageInit.Initializer.class, TestDataStorageInit.Initializer.class, BeanInit.Initializer.class,
        TablesStorageService.class, TablesStorageSetup.Initializer.class})
public class InitializationConfig {
}
