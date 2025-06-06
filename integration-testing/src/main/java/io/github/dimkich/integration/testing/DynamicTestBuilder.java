package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.execution.junit.ExecutionListener;
import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.xml.XmlTestCaseMapperBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.http.converter.json.SpringHandlerInstantiator;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class DynamicTestBuilder {
    private final ConfigurableListableBeanFactory beanFactory;
    private final Assertion assertion;
    private final TestDataStorages testDataStorages;
    private final TestExecutor testExecutor;
    private final JunitExecutable junitExecutable;
    private final List<Module> modules;

    public Stream<DynamicNode> build(String path) throws Exception {
        XmlTestCaseMapperBuilder builder = new XmlTestCaseMapperBuilder();
        builder.path(path);
        for (Module module : modules) {
            module.getSubTypesWithName().forEach(p -> builder.addSubTypes(p.getValue(), p.getKey()));
            module.getSubTypes().forEach(builder::addSubTypes);
            module.getAliases().forEach(p -> builder.addAlias(p.getKey(), p.getValue()));
            module.getJacksonModules().forEach(builder::module);
            module.getJacksonFilters().forEach(builder::filter);
        }
        TestCaseMapper testCaseMapper = builder.build();
        ObjectMapper mapper = (ObjectMapper) testCaseMapper.unwrap();
        mapper.setHandlerInstantiator(new SpringHandlerInstantiator(beanFactory));
        testDataStorages.setTestCaseMapper(testCaseMapper);
        testExecutor.setTestCaseMapper(testCaseMapper);
        junitExecutable.setTestCaseMapper(testCaseMapper);

        TestCase testCase = testCaseMapper.readAllTestCases();
        ExecutionListener.getInstance().setRoot(testCase);
        ExecutionListener.getInstance().setTestExecutor(testExecutor);
        return testCase.getSubTestCases().stream().map(this::toDynamicNode);
    }

    @SneakyThrows
    private DynamicNode toDynamicNode(TestCase testCase) {
        if (testCase.isContainer()) {
            return DynamicContainer.dynamicContainer(testCase.getName(), testCase.getSubTestCases().stream()
                    .map(this::toDynamicNode));
        }
        junitExecutable.setTestCase(testCase);
        return DynamicTest.dynamicTest(testCase.getName(), junitExecutable);
    }
}
