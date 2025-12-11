package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.initialization.bean.BeanInit;
import io.github.dimkich.integration.testing.initialization.bean.BeanInitSetup;
import io.github.dimkich.integration.testing.initialization.date.time.DateTimeInit;
import io.github.dimkich.integration.testing.initialization.date.time.DateTimeInitSetup;
import io.github.dimkich.integration.testing.initialization.key.value.KeyValueStorageInit;
import io.github.dimkich.integration.testing.initialization.key.value.KeyValueStorageInitSetup;
import io.github.dimkich.integration.testing.initialization.mock.MockInit;
import io.github.dimkich.integration.testing.initialization.mock.MockInitSetup;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageInit;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageInitSetup;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageSetup;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageSetupSetup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({InitializationService.class, BeanInitSetup.class, DateTimeInitSetup.class, KeyValueStorageInitSetup.class,
        MockInitSetup.class, SqlStorageInitSetup.class, SqlStorageSetupSetup.class})
public class InitializationConfig {
    @Bean
    TestSetupModule initializationTestSetupModule() {
        return new TestSetupModule()
                .addParentType(TestInit.class)
                .addSubTypes(DateTimeInit.class, KeyValueStorageInit.class, BeanInit.class, MockInit.class,
                        SqlStorageSetup.class, SqlStorageInit.class);
    }
}
