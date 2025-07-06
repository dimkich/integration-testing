package io.github.dimkich.integration.testing.junit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.IntegrationTestConfig;
import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.Module;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

@IntegrationTesting
@SpringBootTest(classes = {IntegrationTestConfig.class, JunitIntegrationTest.Config.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class JunitIntegrationTest {
    private final DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() throws Exception {
        return dynamicTestBuilder.build("junit/junitIntegration.xml");
    }

    @Configuration
    static class Config {
        @Bean
        Module module() {
            return new Module().addSubTypes(Selector.class.getAnnotation(JsonSubTypes.class));
        }

        @Bean
        TestLauncher launcher() {
            return new TestLauncher();
        }
    }

    public static class TestLauncher {
        public List<String> execute(List<Selector> selectors) {
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectors.stream().map(Selector::toSelector).collect(Collectors.toList()))
                    .build());
            assertEquals(1, SimpleTest.getInstance().getLastTestCases().size());
            List<String> result = new ArrayList<>(SimpleTest.getInstance().getExecutedTestCases());
            SimpleTest.getInstance().clear();
            return result;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ClassSelector.class, name = "selClass"),
            @JsonSubTypes.Type(value = UniqueIdSelector.class, name = "selUniqueId"),
    })
    public interface Selector {
        DiscoverySelector toSelector();
    }

    @Getter
    static class ClassSelector implements Selector {
        @JsonValue
        private final Class<?> javaClass;

        public ClassSelector(String javaClass) throws ClassNotFoundException {
            this.javaClass = Class.forName(javaClass);
        }

        @Override
        public DiscoverySelector toSelector() {
            return selectClass(javaClass);
        }
    }

    @Getter
    static class UniqueIdSelector implements Selector {
        @JsonValue
        private final String selector;

        public UniqueIdSelector(String selector) {
            this.selector = selector;
        }

        @Override
        public DiscoverySelector toSelector() {
            return selectUniqueId("[engine:junit-jupiter]/" +
                                  "[class:" + SimpleTest.class.getName() + "]" +
                                  "/[test-factory:tests()]/" + selector);
        }
    }
}
