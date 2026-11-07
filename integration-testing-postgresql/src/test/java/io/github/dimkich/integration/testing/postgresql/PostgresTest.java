package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

@EnableTestPostgreSQL
@SpringBootTest(classes = PostgresTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PostgresTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("postgresql.xml");
    }
}
