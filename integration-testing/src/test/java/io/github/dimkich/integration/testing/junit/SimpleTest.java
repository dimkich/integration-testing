package io.github.dimkich.integration.testing.junit;

import io.github.dimkich.integration.testing.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

@IntegrationTesting
@SpringBootTest(classes = {IntegrationTestConfig.class, SimpleTest.Config.class},
        properties = {"integration.testing.enabled=true"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SimpleTest {
    private final DynamicTestBuilder dynamicTestBuilder;
    @MockBean
    private final Assertion assertion;
    @Getter
    private static SimpleTest instance;
    @Getter
    private final List<String> executedTestCases = new ArrayList<>();
    @Getter
    private final List<String> lastTestCases = new ArrayList<>();

    @TestFactory
    Stream<DynamicNode> tests() throws Exception {
        Mockito.doAnswer(i -> {
                    lastTestCases.add(i.<TestCase>getArgument(1).getName());
                    return null;
                })
                .when(assertion)
                .afterTests(any(TestCaseMapper.class), any(TestCase.class));
        instance = this;
        return dynamicTestBuilder.build("junit/simple.xml");
    }

    public void clear() {
        executedTestCases.clear();
        lastTestCases.clear();
    }

    @Configuration
    static class Config {
        @Bean
        Consumer<?> consumer() {
            return Mockito.mock(Consumer.class, Mockito.withSettings()
                    .defaultAnswer(i -> {
                        SimpleTest.getInstance().executedTestCases.add(i.getArgument(0));
                        return null;
                    }));
        }

        @Bean
        BeforeTestCase beforeTestCase() {
            return tc -> SimpleTest.getInstance()
                    .executedTestCases.add("before " + (tc.getName() == null ? "root" : tc.getName()));
        }

        @Bean
        AfterTestCase afterTestCase() {
            return tc -> SimpleTest.getInstance()
                    .executedTestCases.add("after " + (tc.getName() == null ? "root" : tc.getName()));
        }
    }
}
