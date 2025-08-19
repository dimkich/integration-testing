package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestContainer;
import io.github.dimkich.integration.testing.format.TestMapper;
import io.github.dimkich.integration.testing.util.TestUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;

import javax.xml.stream.Location;
import java.io.FileWriter;
import java.io.IOException;

@RequiredArgsConstructor
public class XmlTestMapper implements TestMapper {
    private final static String fileHeader = "<!-- @" + "formatter:off -->";
    private final XmlMapper xmlMapper;
    private final ObjectToLocationStorage objectToLocationStorage;

    @Setter
    private String path;

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
