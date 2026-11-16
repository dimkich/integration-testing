package io.github.dimkich.integration.testing.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.format.json.JsonTestMapper;
import io.github.dimkich.integration.testing.format.xml.XmlTestMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class CompositeTestMapper {
    private final List<TestMapper> mappers;
    private TestMapper currentMapper;
    private SerializerProvider serializerProvider;

    public void setPath(String path) {
        Class<?> cls = path.endsWith("json") ? JsonTestMapper.class : XmlTestMapper.class;
        TestMapper mapper = findMapper(cls, path);
        mapper.setPath(path);
        currentMapper = mapper;
        serializerProvider = null;
    }

    public String getFilePath() {
        return current().getFilePath();
    }

    public Test readAllTests() throws IOException {
        return current().readAllTests();
    }

    public void writeRootTest(Test test) throws IOException {
        current().writeRootTest(test);
    }

    public String getRootTestAsString(Test test) throws IOException {
        return current().getRootTestAsString(test);
    }

    public String getSingleTestAsString(Test test) throws IOException {
        return current().getSingleTestAsString(test);
    }

    public String getCurrentPathAndLocation(Test test) throws IOException {
        return current().getCurrentPathAndLocation(test);
    }

    public ObjectMapper unwrap() {
        return current().unwrap();
    }

    /**
     * Returns the serializer provider of the current test format. The provider is created on the
     * first call and kept until the format changes.
     *
     * @return serializer provider of the current mapper
     */
    public SerializerProvider getSerializerProvider() {
        SerializerProvider provider = serializerProvider;
        if (provider == null) {
            provider = current().unwrap().getSerializerProviderInstance();
            serializerProvider = provider;
        }
        return provider;
    }

    private TestMapper findMapper(Class<?> type, String path) {
        for (TestMapper mapper : mappers) {
            if (mapper.getClass().equals(type)) {
                return mapper;
            }
        }
        throw new IllegalStateException("No " + type.getSimpleName() + " is configured for path " + path);
    }

    private TestMapper current() {
        if (currentMapper == null) {
            throw new IllegalStateException("Test format is not selected: call setPath first");
        }
        return currentMapper;
    }
}
