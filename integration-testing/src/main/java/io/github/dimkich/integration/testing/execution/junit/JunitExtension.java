package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.execution.TestBeanMock;
import io.github.dimkich.integration.testing.execution.TestConstructorMock;
import io.github.dimkich.integration.testing.execution.TestStaticMock;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationManager;
import io.github.dimkich.integration.testing.openapi.TestOpenAPI;
import io.github.dimkich.integration.testing.web.TestRestTemplate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * JUnit 5 extension that prepares and tears down the integration-testing
 * environment for {@link SpringBootTest}-based tests.
 *
 * <p>Annotated with {@link Order @Order(Integer.MIN_VALUE)} so it is guaranteed
 * to run <b>before</b> {@code SpringExtension}. Without this, a scenario is
 * possible where {@code SpringExtension.beforeAll} creates the Spring context
 * before {@link InstrumentationManager#install(Class)} has populated
 * {@link InstrumentationManager#getActivePlugins()} — and plugins that inspect
 * active instrumentations while the context is being built (for example
 * {@code KafkaInFlightAgent.isInflightActive()} inside
 * {@code KafkaConfig.PostProcessor}) would silently observe an empty list.
 *
 * <p>The ordering is not guaranteed by the JUnit specification nor by source
 * order of annotations on the test class — {@code @Order} removes that
 * dependency. {@code afterAll} callbacks are invoked in reverse order, so the
 * instrumentation teardown in {@link #afterAll} runs after the Spring context is
 * closed rather than before it.
 */
@Slf4j
@Order(Integer.MIN_VALUE)
public class JunitExtension implements BeforeAllCallback, AfterAllCallback {
    @Getter
    private static List<TestOpenAPI> testOpenAPIS = List.of();
    @Getter
    private static List<TestRestTemplate> testRestTemplates = List.of();
    @Getter
    private static List<TestBeanMock> beanMocks = List.of();
    @Getter
    private static List<TestConstructorMock> constructorMocks = List.of();
    @Getter
    private static List<TestStaticMock> staticMocks = List.of();
    @Getter
    private static SpringBootTest springBootTest;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        springBootTest = testClass.getAnnotation(SpringBootTest.class);
        if (springBootTest == null) {
            throw new IllegalStateException("No SpringBootTest Annotation found");
        }
        InstrumentationManager.install(testClass);

        testOpenAPIS = List.of(testClass.getAnnotationsByType(TestOpenAPI.class));
        beanMocks = List.of(testClass.getAnnotationsByType(TestBeanMock.class));
        constructorMocks = List.of(testClass.getAnnotationsByType(TestConstructorMock.class));
        staticMocks = List.of(testClass.getAnnotationsByType(TestStaticMock.class));
        testRestTemplates = List.of(testClass.getAnnotationsByType(TestRestTemplate.class));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        InstrumentationManager.clear();
        PointcutRegistry.clear();
        testOpenAPIS = List.of();
        testRestTemplates = List.of();
        beanMocks = List.of();
        constructorMocks = List.of();
        staticMocks = List.of();
    }
}
