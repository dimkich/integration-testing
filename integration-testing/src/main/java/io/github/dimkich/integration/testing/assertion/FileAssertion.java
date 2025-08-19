package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.*;
import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RequiredArgsConstructor
@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "file")
public class FileAssertion implements Assertion {
    private static final String SETTINGS_FILE = "settings.txt";
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private JunitExecutable executable;

    private final Set<String> initialized = new HashSet<>();
    private final Map<Test, String> map = new HashMap<>();
    private String name;
    private int testIndex = 0;

    @Override
    public void assertTestsEquals(CompositeTestMapper mapper, Test expected, Test actual) throws Exception {
        initialize(mapper.getFilePath());
        String expectedStr = mapper.getSingleTestAsString(expected);
        String actualStr = mapper.getSingleTestAsString(actual);
        if (Objects.equals(expectedStr, actualStr)) {
            return;
        }
        testIndex++;
        String fileExpected = write(expectedStr, testIndex + "_expected.xml");
        String fileActual = write(actualStr, testIndex + "_actual.xml");
        map.put(expected, name + testIndex);
        throw new FileComparisonFailure("error message", "[]", "[]", fileExpected, fileActual);
    }

    @Override
    public void afterTests(CompositeTestMapper mapper, Test rootTest) throws Exception {
        replace(rootTest);
        write(mapper.getRootTestAsString(rootTest), "template.xml");
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

    private String write(String data, String fileNamePostfix) throws IOException {
        String fileName = AssertionConfig.resultDir + File.separator + executable.getTestFullName()
                + File.separator + fileNamePostfix;
        Files.writeString(Paths.get(fileName), data);
        return fileName;
    }

    private void initialize(String path) throws IOException {
        if (initialized.contains(path)) {
            return;
        }
        name = RandomStringUtils.random(16, true, true) + "_";

        String dir = AssertionConfig.resultDir + File.separator + executable.getTestFullName()
                + File.separator;
        Files.createDirectories(Paths.get(dir));
        File[] files = new File(dir).listFiles();
        Arrays.stream(files == null ? new File[0] : files)
                .forEach(FileSystemUtils::deleteRecursively);
        Files.writeString(Paths.get(dir + SETTINGS_FILE),
                path + "\n" + "<testCase name=\"" + name);
        initialized.add(path);
    }
}
