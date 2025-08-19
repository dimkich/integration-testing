package io.github.dimkich.integration.testing.format;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.format.xml.XmlTestMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class CompositeTestMapper {
    private final List<TestMapper> mappers;
    private TestMapper currentMapper;

    public void setPath(String path) {
        Class<?> cls = XmlTestMapper.class;
        for (TestMapper mapper : mappers) {
            if (mapper.getClass().equals(cls)) {
                currentMapper = mapper;
                break;
            }
        }
        currentMapper.setPath(path);
    }

    public String getFilePath() {
        return currentMapper.getFilePath();
    }

    public Test readAllTests() throws IOException {
        return currentMapper.readAllTests();
    }

    public void writeRootTest(Test test) throws IOException {
        currentMapper.writeRootTest(test);
    }

    public String getRootTestAsString(Test test) throws IOException {
        return currentMapper.getRootTestAsString(test);
    }

    public String getSingleTestAsString(Test test) throws IOException {
        return currentMapper.getSingleTestAsString(test);
    }

    public String getCurrentPathAndLocation(Test test) throws IOException {
        return currentMapper.getCurrentPathAndLocation(test);
    }

    public Object unwrap() {
        return currentMapper.unwrap();
    }
}
