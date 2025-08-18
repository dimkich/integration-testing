package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestMapper;
import io.github.dimkich.integration.testing.TestContainer;
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
public class XmlTestMapper implements TestMapper {
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

    public Test readAllTests() throws IOException {
        objectToLocationStorage.start();
        try {
            return xmlMapper.readValue(new ClassPathResource(path).getInputStream(), TestContainer.class);
        } finally {
            objectToLocationStorage.end();
        }
    }

    public void writeRootTest(Test test) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TestUtils.getTestResourceFile(path))) {
            fileWriter.write(getRootTestAsString(test));
        }
    }

    @Override
    public String getRootTestAsString(Test test) throws IOException {
        String text = xmlMapper.writeValueAsString(test);
        if (text.contains(" encoding='UTF-8'?>")) {
            text = text.replace(" encoding='UTF-8'?>", " encoding='UTF-8'?>" + System.lineSeparator() + fileHeader);
        } else {
            text = fileHeader + System.lineSeparator() + text;
        }
        return text;
    }

    public String getSingleTestAsString(Test test) throws IOException {
        return xmlMapper.writeValueAsString(test)
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
        return cloner.clone(object);
    }

    public String getCurrentPathAndLocation(Test test) throws IOException {
        String result = TestUtils.getTestResourceFile(path).getCanonicalPath().replace("\\", "/");
        Location location = objectToLocationStorage.getLocation(test);
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
