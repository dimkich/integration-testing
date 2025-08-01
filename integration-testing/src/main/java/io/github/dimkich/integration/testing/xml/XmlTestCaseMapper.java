package io.github.dimkich.integration.testing.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseMapper;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.util.TestUtils;
import io.github.sugarcubes.cloner.Cloner;
import io.github.sugarcubes.cloner.Cloners;
import io.github.sugarcubes.cloner.ReflectionClonerBuilder;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;

import javax.xml.stream.Location;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class XmlTestCaseMapper implements TestCaseMapper {
    private final static String fileHeader = "<!-- @" + "formatter:off -->";
    private final XmlMapper xmlMapper;
    private final ObjectToLocationStorage objectToLocationStorage;
    private final List<TestSetupModule> modules;

    @Setter
    private String path;
    private Cloner cloner;

    @Override
    public String getFilePath() {
        return TestUtils.getTestResourceFile(path).getPath();
    }

    public TestCase readAllTestCases() throws IOException {
        objectToLocationStorage.start();
        try {
            return xmlMapper.readValue(new ClassPathResource(path).getInputStream(), TestCase.class);
        } finally {
            objectToLocationStorage.end();
        }
    }

    public void writeRootTestCase(TestCase testCase) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TestUtils.getTestResourceFile(path))) {
            fileWriter.write(getRootTestCaseAsString(testCase));
        }
    }

    @Override
    public String getRootTestCaseAsString(TestCase testCase) throws IOException {
        String text = xmlMapper.writeValueAsString(testCase);
        if (text.contains(" encoding='UTF-8'?>")) {
            text = text.replace(" encoding='UTF-8'?>", " encoding='UTF-8'?>" + System.lineSeparator() + fileHeader);
        } else {
            text = fileHeader + System.lineSeparator() + text;
        }
        return text;
    }

    public String getSingleTestCaseAsString(TestCase testCase) throws IOException {
        return xmlMapper.writeValueAsString(testCase)
                .replace("<?xml version='1.1' encoding='UTF-8'?>", "")
                .trim();
    }

    @SneakyThrows
    public <T> T deepClone(T object) {
        if (cloner == null) {
            ReflectionClonerBuilder builder = Cloners.builder();
            for (TestSetupModule module : modules) {
                module.getFieldActions().forEach(builder::fieldAction);
                module.getTypeActions().forEach(builder::typeAction);
                module.getPredicateTypeActions().forEach(builder::typeAction);
            }
            cloner = builder.build();
        }
        if (object instanceof Throwable) {
            return object;
        }
        return cloner.clone(object);
    }

    public String getCurrentPathAndLocation(TestCase testCase) throws IOException {
        String result = TestUtils.getTestResourceFile(path).getCanonicalPath().replace("\\", "/");
        Location location = objectToLocationStorage.getLocation(testCase);
        if (location != null) {
            result += ":" + location.getLineNumber() + ":" + location.getColumnNumber();
        }
        return result;
    }

    @Override
    public XmlMapper unwrap() {
        return xmlMapper;
    }
}
