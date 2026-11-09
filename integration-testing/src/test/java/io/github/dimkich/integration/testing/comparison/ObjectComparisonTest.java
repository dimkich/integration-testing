package io.github.dimkich.integration.testing.comparison;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.common.map.JsonMapAsEntries;
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

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

@IntegrationTesting
@SpringBootTest(classes = ObjectComparisonTest.Config.class)
public class ObjectComparisonTest {
    @Autowired
    private DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("comparison/comparison.xml");
    }

    @Configuration
    static class Config {
        @Bean
        ObjectComparisonTestFacade objectComparisonTestFacade(ObjectComparisonService objectComparisonService) {
            return new ObjectComparisonTestFacade(objectComparisonService);
        }

        @Bean
        TestSetupModule comparisonTestModule() {
            return new TestSetupModule()
                    .addSubTypes(CustomId.class, MapHolder.class, ZonedHolder.class)
                    .addEqualsForType(CustomId.class, (id1, id2) -> id1.getValue().equals(id2.getValue()));
        }
    }

    @RequiredArgsConstructor
    public static class ObjectComparisonTestFacade {
        private final ObjectComparisonService objectComparisonService;

        public boolean compare(Object actual, Object expected) {
            return objectComparisonService.isEquals(actual, expected);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomId {
        private String value;
        private String tag;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapHolder {
        @JsonProperty("entry")
        @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
        private LinkedHashMap<BigDecimal, String> entries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZonedHolder {
        private ZonedDateTime value;
    }
}
