package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@RequiredArgsConstructor
@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "SingleFile")
public class SingleFileAssertion implements Assertion {
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private TestExecutor testExecutor;

    @Override
    public boolean makeTestDeepClone() {
        return false;
    }

    @Override
    public boolean useTestTempDir() {
        return true;
    }

    @Override
    public void assertTestsEquals(CompositeTestMapper mapper, Test expected, Test actual) {
    }

    @Override
    public void afterTests(CompositeTestMapper mapper, Test rootTest) throws Exception {
        String actual = mapper.getRootTestAsString(rootTest);
        String expected = Files.readString(Path.of(mapper.getFilePath()));
        if (Objects.equals(actual, expected)) {
            return;
        }
        Path dir = testExecutor.getTestsDir();
        FileSystemUtils.deleteRecursively(dir);
        Files.createDirectories(dir);
        Path actualFile = dir.resolve("actual.xml");
        Files.writeString(actualFile, actual);
        throw new FileComparisonFailure("error message", "[]", "[]", mapper.getFilePath(),
                actualFile.toString());
    }
}
