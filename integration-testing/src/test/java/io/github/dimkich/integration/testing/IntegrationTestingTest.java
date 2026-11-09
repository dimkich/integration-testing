package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dimkich.integration.testing.execution.TestBeanMock;
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
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.stream.Stream;

@SpringBootTest(classes = IntegrationTestingTest.Config.class)
@TestBeanMock(mockClass = IntegrationTestingTest.Converter.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class IntegrationTestingTest {
    private final DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("integrationTesting.xml");
    }

    @Configuration
    @Import(Converter.class)
    static class Config {
        @Bean
        TestSetupModule testModule() {
            return new TestSetupModule().addSubTypes(Amount.class, SortedBook.class, UnorderedBook.class);
        }
    }

    @Component("converter")
    public static class Converter {
        public String convertToString(Integer integer) {
            return String.valueOf(integer);
        }

        public String charge(BigDecimal amount) {
            return "real " + amount;
        }

        public String amount(Amount amount) {
            return "real " + amount;
        }

        public String sorted(SortedBook book) {
            return "real " + book;
        }

        public String unordered(UnorderedBook book) {
            return "real " + book;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Amount {
        private BigDecimal value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortedBook {
        @JsonProperty("bid")
        @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
        private TreeMap<BigDecimal, BigDecimal> bids;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnorderedBook {
        @JsonProperty("entry")
        @JsonMapAsEntries(entryFormat = JsonMapAsEntries.EntryFormat.KEY_AS_ATTRIBUTE)
        private LinkedHashMap<BigDecimal, String> entries;
    }
}
