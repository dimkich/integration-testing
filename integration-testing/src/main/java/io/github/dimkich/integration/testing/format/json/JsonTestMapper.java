package io.github.dimkich.integration.testing.format.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestContainer;
import io.github.dimkich.integration.testing.format.TestMapper;
import io.github.dimkich.integration.testing.util.TestUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;

import java.io.FileWriter;
import java.io.IOException;

@RequiredArgsConstructor
public class JsonTestMapper implements TestMapper {
    private final ObjectMapper objectMapper;
    private final static String fileHeader = "/* @" + "formatter:off */";
    @Setter
    private String path;

    @Override
    public String getFilePath() {
        return TestUtils.getTestResourceFile(path).getPath();
    }

    @Override
    public Test readAllTests() throws IOException {
        return objectMapper.readValue(new ClassPathResource(path).getInputStream(), TestContainer.class);
    }

    @Override
    public void writeRootTest(Test test) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TestUtils.getTestResourceFile(path))) {
            fileWriter.write(getRootTestAsString(test));
        }
    }

    @Override
    public String getRootTestAsString(Test test) throws IOException {
        return fileHeader + System.lineSeparator() + objectMapper.writerFor(Test.class).writeValueAsString(test);
    }

    @Override
    public String getSingleTestAsString(Test test) throws IOException {
        return objectMapper.writerFor(Test.class).writeValueAsString(test);
    }

    @Override
    public String getCurrentPathAndLocation(Test test) throws IOException {
        return TestUtils.getTestResourceFile(path).getCanonicalPath().replace("\\", "/")
                + ":" + test.getLineNumber() + ":" + test.getColumnNumber();
    }

    @Override
    public Object unwrap() {
        return objectMapper;
    }
}
