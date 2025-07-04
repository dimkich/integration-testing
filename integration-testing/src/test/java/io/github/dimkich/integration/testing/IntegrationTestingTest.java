package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.execution.TestCaseBeanMocks;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@SpringBootTest(classes = {IntegrationTestConfig.class, IntegrationTestingTest.Config.class},
        properties = {"integration.testing.enabled=true"})
@TestCaseBeanMocks(mockClasses = IntegrationTestingTest.Converter.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class IntegrationTestingTest {
    private final DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() throws Exception {
        return dynamicTestBuilder.build("integrationTesting.xml");
    }

    @Configuration
    @Import(Converter.class)
    static class Config {
    }

    @Component("converter")
    public static class Converter {
        public String convertToString(Integer integer) {
            return String.valueOf(integer);
        }
    }
}
