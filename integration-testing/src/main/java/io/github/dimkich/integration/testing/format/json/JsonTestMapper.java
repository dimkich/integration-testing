package io.github.dimkich.integration.testing.format.json;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.format.TestMapper;

import java.io.IOException;

public class JsonTestMapper extends TestMapper {
    private final static String fileHeader = "/* @" + "formatter:off */";

    public JsonTestMapper(JsonMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String getRootTestAsString(Test test) throws IOException {
        return fileHeader + System.lineSeparator() + super.getRootTestAsString(test);
    }

    @Override
    public JsonMapper unwrap() {
        return (JsonMapper) super.unwrap();
    }
}
