package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.InstrumentationManager;
import io.github.dimkich.integration.testing.RepeatInstrumentation;
import io.github.dimkich.integration.testing.date.time.MockJavaTime;
import io.github.dimkich.integration.testing.date.time.MockJavaTimeSetUp;
import io.github.dimkich.integration.testing.execution.TestBeanMock;
import io.github.dimkich.integration.testing.execution.TestConstructorMock;
import io.github.dimkich.integration.testing.execution.TestStaticMock;
import io.github.dimkich.integration.testing.execution.mokito.MockitoGlobal;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.openapi.TestOpenAPI;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletionManager;
import io.github.dimkich.integration.testing.web.TestRestTemplate;
import io.github.sugarcubes.cloner.ClonerAgentSetUp;
import lombok.Getter;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

/**
 * JUnit 5 extension that prepares and tears down the integration-testing
 * environment for {@link SpringBootTest}-based tests.
 * <p>
 * On {@link #beforeAll(ExtensionContext)} it:
 * <ul>
 *     <li>verifies that the test class is annotated with {@link SpringBootTest}</li>
 *     <li>installs a {@link Instrumentation} instance via {@link ByteBuddyAgent}</li>
 *     <li>moves required helper classes to the boot classloader</li>
 *     <li>initialises mocked Java time if {@link MockJavaTime} is present</li>
 *     <li>configures ByteBuddy agents for the different wait-completion strategies</li>
 *     <li>collects OpenAPI, REST template and mock annotations declared on the test class</li>
 * </ul>
 * On {@link #afterAll(ExtensionContext)} it reverts all instrumentation state and clears
 * the collected metadata to avoid leaking state between tests.
 */
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

    private static Instrumentation instrumentation;
    private static final InstrumentationManager instrumentationManager = new InstrumentationManager();

    /**
     * Prepares the integration-testing infrastructure before all tests of the
     * current test class are executed.
     *
     * @param context JUnit extension context providing access to the test class
     * @throws Exception if instrumentation or agent setup fails
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        springBootTest = testClass.getAnnotation(SpringBootTest.class);
        if (springBootTest == null) {
            throw new IllegalStateException("No SpringBootTest Annotation found");
        }
        MockitoGlobal.start();
        instrumentation = ByteBuddyAgent.install();
        ClonerAgentSetUp.setClonerInstrumentationIfNone(instrumentation);
        MockJavaTime mockJavaTime = testClass.getAnnotation(MockJavaTime.class);
        if (mockJavaTime != null) {
            MockJavaTimeSetUp.setUp(mockJavaTime);
        }
        AgentBuilder builder = instrumentationManager.createAgentBuilder();
        builder = WaitCompletionManager.setUp(testClass, builder);
        instrumentationManager.install(builder, instrumentation);
        repeatInstrumentation(testClass);
        testOpenAPIS = List.of(testClass.getAnnotationsByType(TestOpenAPI.class));
        beanMocks = List.of(testClass.getAnnotationsByType(TestBeanMock.class));
        constructorMocks = List.of(testClass.getAnnotationsByType(TestConstructorMock.class));
        staticMocks = List.of(testClass.getAnnotationsByType(TestStaticMock.class));
        testRestTemplates = List.of(testClass.getAnnotationsByType(TestRestTemplate.class));
    }

    /**
     * Cleans up all integration-testing related state after all tests of the
     * current test class have been executed.
     *
     * @param context JUnit extension context (not used, but part of the contract)
     */
    @Override
    public void afterAll(ExtensionContext context) {
        MockitoGlobal.stop();
        MockJavaTimeSetUp.tearDown();
        instrumentationManager.reset(instrumentation);
        WaitCompletionManager.tearDown();
        PointcutRegistry.clear();
        testOpenAPIS = List.of();
        testRestTemplates = List.of();
        beanMocks = List.of();
        constructorMocks = List.of();
        staticMocks = List.of();
    }

    /**
     * Re-applies ByteBuddy instrumentation for all loaded classes whose name
     * starts with any prefix specified in the {@link RepeatInstrumentation}
     * annotation on the given test class.
     *
     * @param testClass the test class that may declare {@link RepeatInstrumentation}
     * @throws UnmodifiableClassException if a matched class cannot be retransformed
     */
    private static void repeatInstrumentation(Class<?> testClass) throws UnmodifiableClassException {
        RepeatInstrumentation ri = findMergedAnnotation(testClass, RepeatInstrumentation.class);
        if (ri == null) {
            return;
        }
        Set<Class<?>> classes = new HashSet<>();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (!instrumentation.isModifiableClass(clazz)) {
                continue;
            }
            for (String name : ri.value()) {
                if (clazz.getName().startsWith(name)) {
                    classes.add(clazz);
                }
            }
        }
        if (!classes.isEmpty()) {
            instrumentation.retransformClasses(classes.toArray(new Class[]{}));
        }
    }
}