package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.*;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "file")
public class FileAssertion implements Assertion {
    private static final String SETTINGS_FILE = "settings.txt";
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private TestExecutor executor;

    private final Set<Path> initialized = new HashSet<>();
    private final Map<Test, String> map = new HashMap<>();
    private String name;
    private int testIndex = 0;

    @Override
    public boolean useTestTempDir() {
        return true;
    }

    @Override
    public void assertTestsEquals(CompositeTestMapper mapper, Test expected, Test actual) throws Exception {
        String expectedStr = mapper.getSingleTestAsString(expected);
        String actualStr = mapper.getSingleTestAsString(actual);
        if (Objects.equals(expectedStr, actualStr)) {
            return;
        }
        testIndex++;
        Path dir = executor.getTestsDir();
        if (name == null) {
            name = RandomStringUtils.random(16, true, true) + "_";
        }
        if (!initialized.contains(dir)) {
            initialized.add(dir);
            FileSystemUtils.deleteRecursively(dir);
            Files.createDirectories(dir);
        }
        String fileExpected = writeFile(dir.resolve(testIndex + "_expected.xml"), expectedStr);
        String fileActual = writeFile(dir.resolve(testIndex + "_actual.xml"), actualStr);
        map.put(expected, name + testIndex);
        throw new FileComparisonFailure("error message", "[]", "[]", fileExpected, fileActual);
    }

    @Override
    public void afterTests(CompositeTestMapper mapper, Test rootTest) throws Exception {
        if (testIndex > 0) {
            replace(rootTest);
            Path dir = executor.getTestsDir();
            initialized.remove(dir);
            writeFile(dir.resolve(SETTINGS_FILE), mapper.getFilePath() + "\n" + name);
            writeFile(dir.resolve("template.xml"), mapper.getRootTestAsString(rootTest));
        }
    }

    private void replace(Test test) {
        for (int i = 0; i < test.getSubTests().size(); i++) {
            Test sub = test.getSubTests().get(i);
            if (map.containsKey(sub)) {
                Test link = switch (sub.getType()) {
                    case TestContainer -> new TestContainer();
                    case TestCase -> new TestCase();
                    case TestPart -> new TestPart();
                };
                link.setName(map.get(sub));
                test.getSubTests().set(i, link);
            } else {
                replace(sub);
            }
        }
    }

    private String writeFile(Path fileName, String data) throws IOException {
        Files.writeString(fileName, data);
        return fileName.toString();
    }
}
