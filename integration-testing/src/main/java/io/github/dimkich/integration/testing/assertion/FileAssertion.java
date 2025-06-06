package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseMapper;
import io.github.dimkich.integration.testing.execution.junit.ExecutionListener;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RequiredArgsConstructor
@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "file")
public class FileAssertion implements Assertion {
    private final FileOperations fileOperations;
    private static final String SETTINGS_FILE = "settings.txt";
    private final Set<String> initialized = new HashSet<>();
    private String name;
    private int testCaseIndex = 0;
    private final Map<TestCase, String> map = new HashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    void init() {
        fileOperations.clearTestsDir();
    }

    @Override
    public void assertTestCaseEquals(TestCaseMapper mapper, TestCase expected, TestCase actual) throws Exception {
        initialize(mapper.getFilePath());
        String expectedStr = mapper.getSingleTestCaseAsString(expected);
        String actualStr = mapper.getSingleTestCaseAsString(actual);
        if (Objects.equals(expectedStr, actualStr)) {
            return;
        }
        testCaseIndex++;
        String fileExpected = write(expectedStr, testCaseIndex + "_expected.xml");
        String fileActual = write(actualStr, testCaseIndex + "_actual.xml");
        map.put(expected, name + testCaseIndex);
        throw new FileComparisonFailure("error message", "[]", "[]", fileExpected, fileActual);
    }

    @Override
    public void afterTests(TestCaseMapper mapper, TestCase rootTestCase) throws Exception {
        replace(rootTestCase);
        write(mapper.getRootTestCaseAsString(rootTestCase), "template.xml");
    }

    private void replace(TestCase testCase) {
        for (int i = 0; i < testCase.getSubTestCases().size(); i++) {
            TestCase sub = testCase.getSubTestCases().get(i);
            if (map.containsKey(sub)) {
                TestCase link = new TestCase();
                link.setName(map.get(sub));
                testCase.getSubTestCases().set(i, link);
            } else {
                replace(sub);
            }
        }
    }

    private String write(String data, String fileNamePostfix) throws IOException {
        String fileName = AssertionConfig.resultDir + File.separator + ExecutionListener.getInstance().getTestFullName()
                          + File.separator + fileNamePostfix;
        Files.writeString(Paths.get(fileName), data);
        return fileName;
    }

    private void initialize(String path) throws IOException {
        if (initialized.contains(path)) {
            return;
        }
        name = RandomStringUtils.random(16, true, true) + "_";

        String dir = AssertionConfig.resultDir + File.separator + ExecutionListener.getInstance().getTestFullName()
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
