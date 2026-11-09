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
 * Verifies that disabling {@link DeterministicCollections.Feature#HASH_COLLECTIONS} keeps
 * {@code new HashMap}/{@code new HashSet} untouched while the other replacements stay active.
 */
@DeterministicCollections(
        value = "io.github.dimkich.integration.testing.collections.probe",
        disabled = DeterministicCollections.Feature.HASH_COLLECTIONS)
@RepeatInstrumentation({"io.github.dimkich.integration.testing.collections.probe"})
@SpringBootTest(classes = DeterministicCollectionsDisabledTest.Config.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DeterministicCollectionsDisabledTest {

    private final DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("collections/deterministicCollectionsDisabled.xml");
    }

    @Configuration
    static class Config {
        @Bean
        CollectionsProbe collectionsProbe() {
            return new CollectionsProbe();
        }
    }
}
