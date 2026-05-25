package io.github.dimkich.integration.testing.config;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.TestSetupModule;
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

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@IntegrationTesting
@SpringBootTest(classes = PropertyInheritanceTest.Config.class)
public class PropertyInheritanceTest {

    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() throws Exception {
        return dynamicTestBuilder.build("config/property-inheritance.xml");
    }

    @Configuration
    @Import({PropertyInheritanceMerger.class, PropertyInheritanceTestFacade.class})
    static class Config {
        @Bean
        TestSetupModule module() {
            return new TestSetupModule()
                    .addSubTypes(SimpleConfig.class, ExclusiveConfig.class, NestedContainer.class,
                            TripleExclusiveConfig.class, SetContainer.class, ComplexItem.class);
        }
    }

    @RequiredArgsConstructor
    @Component("propertyInheritanceTestFacade")
    public static class PropertyInheritanceTestFacade {
        private final Cloner cloner;
        private final PropertyInheritanceMerger merger;

        public Object merge(Object target, Object source) {
            target = cloner.clone(target);
            merger.merge(target, source);
            return target;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleConfig {
        private String name;
        private Integer timeout;
        private Set<String> tags;
        private Map<String, String> meta;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExclusiveConfig {
        @PropertyInheritanceExclusive("reference")
        private String beanRef;

        @PropertyInheritanceExclusive("reference")
        private String classRef;

        private String other;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NestedContainer {
        private SimpleConfig config;
        private String globalId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripleExclusiveConfig {
        @PropertyInheritanceExclusive("g1")
        private String a;
        @PropertyInheritanceExclusive("g1")
        private String b;
        @PropertyInheritanceExclusive("g1")
        private String c;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetContainer {
        private Set<ComplexItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexItem {
        private String id;
        private String value;
    }
}