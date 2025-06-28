package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseMapper;
import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@RequiredArgsConstructor
@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "singleFile")
public class SingleFileAssertion implements Assertion {
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private JunitExecutable executable;

    @Override
    public boolean makeTestCaseDeepClone() {
        return false;
    }

    @Override
    public void assertTestCaseEquals(TestCaseMapper mapper, TestCase expected, TestCase actual) {
    }

    @Override
    public void afterTests(TestCaseMapper mapper, TestCase rootTestCase) throws Exception {
        String actual = mapper.getRootTestCaseAsString(rootTestCase);
        String expected = Files.readString(Path.of(mapper.getFilePath()));
        if (Objects.equals(actual, expected)) {
            return;
        }
        Files.createDirectories(Paths.get(AssertionConfig.resultDir));
        String fileName = AssertionConfig.resultDir + executable.getTestFullName() + ".xml";
        Files.writeString(Paths.get(fileName), actual);
        throw new FileComparisonFailure("error message", "[]", "[]", mapper.getFilePath(), fileName);
    }
}
