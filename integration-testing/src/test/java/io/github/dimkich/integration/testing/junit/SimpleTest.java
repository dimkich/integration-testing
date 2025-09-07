package io.github.dimkich.integration.testing.junit;

import io.github.dimkich.integration.testing.*;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
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
@SpringBootTest(classes = {IntegrationTestConfig.class, SimpleTest.Config.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SimpleTest {
    private final DynamicTestBuilder dynamicTestBuilder;
    @MockBean
    private final Assertion assertion;
    @Getter
    private static SimpleTest instance;
    @Getter
    private final List<String> executedTests = new ArrayList<>();

    @TestFactory
    Stream<DynamicNode> tests() throws Exception {
        Mockito.doAnswer(i -> {
                    Test tc = i.getArgument(1, Test.class);
                    executedTests.add("afterTests " + (tc.getName() == null ? "root" : tc.getName()));
                    return null;
                })
                .when(assertion)
                .afterTests(any(CompositeTestMapper.class), any(Test.class));
        instance = this;
        return dynamicTestBuilder.build("junit/simple.xml");
    }

    public void clear() {
        executedTests.clear();
    }

    @Configuration
    static class Config {
        @Bean
        Consumer<?> consumer() {
            return Mockito.mock(Consumer.class, Mockito.withSettings()
                    .defaultAnswer(i -> {
                        SimpleTest.getInstance().executedTests.add(i.getArgument(0));
                        return null;
                    }));
        }

        @Bean
        BeforeTest beforeTest() {
            return tc -> SimpleTest.getInstance()
                    .executedTests.add("before " + (tc.getName() == null ? "root" : tc.getName()));
        }

        @Bean
        AfterTest afterTest() {
            return tc -> SimpleTest.getInstance()
                    .executedTests.add("after " + (tc.getName() == null ? "root" : tc.getName()));
        }
    }
}
