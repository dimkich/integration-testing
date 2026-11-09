package io.github.dimkich.integration.testing.collections;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.collections.probe.CollectionsProbe;
import io.github.dimkich.integration.testing.instrumentation.RepeatInstrumentation;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

/**
 * Verifies that {@link DeterministicCollections} replaces order-sensitive JDK collection
 * factories and hash-based constructors with ordered analogues.
 */
@DeterministicCollections("io.github.dimkich.integration.testing.collections.probe")
@RepeatInstrumentation({"io.github.dimkich.integration.testing.collections.probe"})
@SpringBootTest(classes = DeterministicCollectionsTest.Config.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DeterministicCollectionsTest {

    private final DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("collections/deterministicCollections.xml");
    }

    @Configuration
    static class Config {
        @Bean
        CollectionsProbe collectionsProbe() {
            return new CollectionsProbe();
        }
    }
}
