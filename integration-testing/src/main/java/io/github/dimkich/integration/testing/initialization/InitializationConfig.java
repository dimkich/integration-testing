package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.initialization.bean.BeanInit;
import io.github.dimkich.integration.testing.initialization.bean.BeanInitSetup;
import io.github.dimkich.integration.testing.initialization.bean.property.BeanPropertyInit;
import io.github.dimkich.integration.testing.initialization.bean.property.BeanPropertyInitSetup;
import io.github.dimkich.integration.testing.initialization.date.time.DateTimeInit;
import io.github.dimkich.integration.testing.initialization.date.time.DateTimeInitSetup;
import io.github.dimkich.integration.testing.initialization.key.value.KeyValueStorageInit;
import io.github.dimkich.integration.testing.initialization.key.value.KeyValueStorageInitSetup;
import io.github.dimkich.integration.testing.initialization.sql.OnTableChangeInit;
import io.github.dimkich.integration.testing.initialization.sql.OnTableChangeInitSetup;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageInit;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageInitSetup;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageSetup;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageSetupSetup;
import io.github.dimkich.integration.testing.initialization.stub.StubInit;
import io.github.dimkich.integration.testing.initialization.stub.StubInitSetup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({InitializationService.class, BeanInitSetup.class, DateTimeInitSetup.class, KeyValueStorageInitSetup.class,
        StubInitSetup.class, SqlStorageInitSetup.class, SqlStorageSetupSetup.class, OnTableChangeInitSetup.class,
        BeanPropertyInitSetup.class})
public class InitializationConfig {
    @Bean
    TestSetupModule initializationTestSetupModule() {
        return new TestSetupModule()
                .addParentType(TestInit.class)
                .addSubTypes(DateTimeInit.class, KeyValueStorageInit.class, BeanInit.class, StubInit.class,
                        SqlStorageSetup.class, SqlStorageInit.class, OnTableChangeInit.class, BeanPropertyInit.class);
    }
}
