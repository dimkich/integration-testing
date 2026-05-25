package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionTree;
import io.github.sugarcubes.cloner.Cloner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@IntegrationTesting
@SpringBootTest(classes = FieldExclusionTest.Config.class)
public class FieldExclusionTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() throws Exception {
        return dynamicTestBuilder.build("storage/fieldExclusion.xml");
    }

    @Configuration
    @Import({FieldExclusionTestFacade.class})
    static class Config {
        @Bean
        TestSetupModule module() {
            return new TestSetupModule()
                    .addSubTypes(TestUser.class, TestProfile.class, TestTag.class, TestArrayContainer.class);
        }
    }

    @RequiredArgsConstructor
    @Component("fieldExclusionTestFacade")
    public static class FieldExclusionTestFacade {
        private final FieldExclusionProcessor fieldExclusionProcessor;
        private final Cloner cloner;

        public Object apply(Object data, List<String> paths) {
            data = cloner.clone(data);
            FieldExclusionTree tree = fieldExclusionProcessor.compile(paths);
            tree.process(data);
            return data;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestUser {
        private int id;
        private String name;
        private boolean active;
        private char category;
        private TestProfile profile;
        private List<TestTag> tags;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestProfile {
        private String email;
        private int rank;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestTag {
        private String code;
        private int priority;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestArrayContainer {
        private TestTag[] tagArray;
    }
}
