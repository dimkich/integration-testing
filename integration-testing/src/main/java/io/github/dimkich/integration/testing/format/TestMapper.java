package io.github.dimkich.integration.testing.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestContainer;
import io.github.dimkich.integration.testing.util.TestUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public abstract class TestMapper {
    private final ObjectMapper objectMapper;
    @Setter
    private String path;

    public String getFilePath() {
        return TestUtils.getTestResourceFile(path).getPath();
    }

    public Test readAllTests() throws IOException {
        File file = new File(getFilePath());
        if (file.exists()) {
            return objectMapper.readValue(file, TestContainer.class);
        }
        return objectMapper.readValue(new ClassPathResource(path).getInputStream(), TestContainer.class);
    }

    public void writeRootTest(Test test) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TestUtils.getTestResourceFile(path), StandardCharsets.UTF_8)) {
            fileWriter.write(getRootTestAsString(test));
        }
    }

    public String getRootTestAsString(Test test) throws IOException {
        return objectMapper.writerFor(Test.class).writeValueAsString(test);
    }

    public String getSingleTestAsString(Test test) throws IOException {
        return objectMapper.writerFor(Test.class).writeValueAsString(test);
    }

    public String getCurrentPathAndLocation(Test test) throws IOException {
        return TestUtils.getTestResourceFile(path).getCanonicalPath().replace("\\", "/")
                + ":" + test.getLineNumber() + ":" + test.getColumnNumber();
    }

    public Object unwrap() {
        return objectMapper;
    }
}
