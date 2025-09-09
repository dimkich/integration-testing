package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.format.TestMapper;

import java.io.IOException;

public class XmlTestMapper extends TestMapper {
    private final static String fileHeader = "<!-- @" + "formatter:off -->";

    public XmlTestMapper(XmlMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String getRootTestAsString(Test test) throws IOException {
        return "<?xml version='1.1' encoding='UTF-8'?>" + System.lineSeparator() + fileHeader
                + removeFirstLine(super.getRootTestAsString(test));
    }

    public String getSingleTestAsString(Test test) throws IOException {
        return removeFirstLine(super.getRootTestAsString(test));
    }

    private String removeFirstLine(String text) {
        int i = text.indexOf('\n');
        while (Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return text.substring(i);
    }

    @Override
    public XmlMapper unwrap() {
        return (XmlMapper) super.unwrap();
    }
}
